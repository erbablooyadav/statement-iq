import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { initializeApp } from 'firebase/app';
import {
  getAuth,
  onAuthStateChanged,
  signInWithPopup,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  signOut,
} from 'firebase/auth';
import type { User as FirebaseUser, Auth } from 'firebase/auth';
import { apiClient, setTokenProvider } from '../api/client';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || '',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || '',
};

// Graceful Firebase initialization — don't crash if keys are missing
let auth: Auth | null = null;
let googleProvider: GoogleAuthProvider | null = null;
const isFirebaseConfigured = !!firebaseConfig.apiKey && firebaseConfig.apiKey !== '';

if (isFirebaseConfigured) {
  try {
    const app = initializeApp(firebaseConfig);
    auth = getAuth(app);
    googleProvider = new GoogleAuthProvider();
  } catch (error) {
    console.warn('Firebase initialization failed:', error);
  }
}

if (!isFirebaseConfigured) {
  console.warn('⚠️ Firebase not configured. Auth is disabled. Set VITE_FIREBASE_* env vars to enable.');
}

interface AuthUser {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  plan?: 'FREE' | 'PRO';
  mongoId?: string;
}

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  isAuthAvailable: boolean;
  loginWithGoogle: () => Promise<void>;
  loginWithEmail: (email: string, password: string) => Promise<void>;
  signupWithEmail: (email: string, password: string, name: string) => Promise<void>;
  logout: () => Promise<void>;
  getToken: () => Promise<string | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(isFirebaseConfigured);

  useEffect(() => {
    if (!auth) {
      setLoading(false);
      return;
    }

    // Wire up the token provider so all apiClient calls get JWT tokens
    setTokenProvider(async () => {
      const currentUser = auth?.currentUser;
      if (!currentUser) return null;
      return currentUser.getIdToken();
    });

    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser: FirebaseUser | null) => {
      if (firebaseUser) {
        const authUser: AuthUser = {
          uid: firebaseUser.uid,
          email: firebaseUser.email,
          displayName: firebaseUser.displayName,
          photoURL: firebaseUser.photoURL,
        };

        // Sync user to backend
        try {
          const token = await firebaseUser.getIdToken();
          const response = await apiClient.post('/auth/sync', {
            email: firebaseUser.email,
            name: firebaseUser.displayName || firebaseUser.email?.split('@')[0],
            photoUrl: firebaseUser.photoURL,
          }, {
            headers: { Authorization: `Bearer ${token}` },
          });

          if (response.data.success) {
            authUser.plan = response.data.data.plan;
            authUser.mongoId = response.data.data.id;
          }
        } catch (error) {
          console.error('Failed to sync user:', error);
        }

        setUser(authUser);
      } else {
        setUser(null);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const loginWithGoogle = async () => {
    if (!auth || !googleProvider) throw new Error('Firebase auth not configured');
    await signInWithPopup(auth, googleProvider);
  };

  const loginWithEmail = async (email: string, password: string) => {
    if (!auth) throw new Error('Firebase auth not configured');
    await signInWithEmailAndPassword(auth, email, password);
  };

  const signupWithEmail = async (email: string, password: string, _name: string) => {
    if (!auth) throw new Error('Firebase auth not configured');
    await createUserWithEmailAndPassword(auth, email, password);
  };

  const logout = async () => {
    if (auth) await signOut(auth);
    setUser(null);
  };

  const getToken = async (): Promise<string | null> => {
    if (!auth) return null;
    const currentUser = auth.currentUser;
    if (!currentUser) return null;
    return currentUser.getIdToken();
  };

  return (
    <AuthContext.Provider value={{ user, loading, isAuthAvailable: isFirebaseConfigured, loginWithGoogle, loginWithEmail, signupWithEmail, logout, getToken }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
