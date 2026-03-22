package com.statementiq.service.domain;

import com.statementiq.dto.UserSyncRequest;
import com.statementiq.exception.ResourceNotFoundException;
import com.statementiq.model.User;
import com.statementiq.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Sync Firebase user to MongoDB on first login.
     * If user already exists, update name/photo and return existing.
     */
    public User syncUser(String firebaseUid, UserSyncRequest request) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .map(existing -> {
                    existing.setName(request.getName());
                    existing.setPhotoUrl(request.getPhotoUrl());
                    log.info("User synced (existing): uid={}", firebaseUid);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .firebaseUid(firebaseUid)
                            .email(request.getEmail())
                            .name(request.getName())
                            .photoUrl(request.getPhotoUrl())
                            .plan(User.Plan.FREE)
                            .totalStatementsUploaded(0)
                            .todayUploads(0)
                            .build();
                    log.info("New user created: uid={}, email={}", firebaseUid, request.getEmail());
                    return userRepository.save(newUser);
                });
    }

    /**
     * Get user by Firebase UID. Used in all authenticated endpoints.
     */
    public User getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found. Please sync your account first."));
    }

    /**
     * Get user by MongoDB ID.
     */
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Check if user can upload (rate limit check).
     * Resets daily counter if date changed.
     */
    public boolean canUpload(User user) {
        String today = LocalDate.now().toString();
        if (!today.equals(user.getLastUploadDate())) {
            user.setTodayUploads(0);
            user.setLastUploadDate(today);
            userRepository.save(user);
        }

        int limit = user.getPlan() == User.Plan.PRO ? 30 : 5;
        return user.getTodayUploads() < limit;
    }

    /**
     * Increment upload counter after successful upload.
     */
    public void incrementUploadCount(User user) {
        String today = LocalDate.now().toString();
        if (!today.equals(user.getLastUploadDate())) {
            user.setTodayUploads(1);
            user.setLastUploadDate(today);
        } else {
            user.setTodayUploads(user.getTodayUploads() + 1);
        }
        user.setTotalStatementsUploaded(user.getTotalStatementsUploaded() + 1);
        userRepository.save(user);
    }

    /**
     * Check if user has Pro plan.
     */
    public boolean isPro(User user) {
        return user.getPlan() == User.Plan.PRO;
    }

    /**
     * Upgrade user to Pro plan.
     */
    public User upgradeToPro(String userId, String razorpaySubscriptionId) {
        User user = getUserById(userId);
        user.setPlan(User.Plan.PRO);
        user.setRazorpaySubscriptionId(razorpaySubscriptionId);
        user.setSubscriptionStatus(User.SubscriptionStatus.ACTIVE);
        return userRepository.save(user);
    }

    /**
     * Downgrade user from Pro.
     */
    public User downgradeToFree(String userId) {
        User user = getUserById(userId);
        user.setPlan(User.Plan.FREE);
        user.setSubscriptionStatus(User.SubscriptionStatus.CANCELLED);
        return userRepository.save(user);
    }

    /**
     * Update notification preferences.
     */
    public User updateNotificationPreferences(String userId, User.NotificationPreferences prefs) {
        User user = getUserById(userId);
        user.setNotificationPreferences(prefs);
        return userRepository.save(user);
    }

    /**
     * Save user — used by controllers that update user fields directly.
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Delete user and all associated data (DPDP Act compliance).
     */
    public void deleteUser(String firebaseUid) {
        userRepository.findByFirebaseUid(firebaseUid).ifPresent(user -> {
            log.info("Deleting user account: uid={}", firebaseUid);
            userRepository.delete(user);
        });
    }
}
