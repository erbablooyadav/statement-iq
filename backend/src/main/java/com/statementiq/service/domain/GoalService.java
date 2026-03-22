package com.statementiq.service.domain;

import com.statementiq.exception.ProPlanRequiredException;
import com.statementiq.exception.ResourceNotFoundException;
import com.statementiq.model.Goal;
import com.statementiq.model.GoalCheckIn;
import com.statementiq.model.User;
import com.statementiq.repository.GoalCheckInRepository;
import com.statementiq.repository.GoalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);
    private final GoalRepository goalRepository;
    private final GoalCheckInRepository checkInRepository;

    public GoalService(GoalRepository goalRepository, GoalCheckInRepository checkInRepository) {
        this.goalRepository = goalRepository;
        this.checkInRepository = checkInRepository;
    }

    /**
     * Create a new saving goal.
     * Free: 1 active goal max, Pro: 5 active goals max.
     */
    public Goal createGoal(String userId, Goal goal, User user) {
        List<Goal> existingGoals = goalRepository.findByUserId(userId);
        long activeCount = existingGoals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE).count();
        int maxGoals = user.getPlan() == User.Plan.PRO ? 5 : 1;

        if (activeCount >= maxGoals) {
            if (user.getPlan() == User.Plan.FREE) {
                throw new ProPlanRequiredException("Free plan allows 1 active goal. Upgrade to Pro for up to 5 goals.");
            }
            throw new ProPlanRequiredException("Maximum 5 active goals reached.");
        }

        goal.setUserId(userId);
        goal.setStatus(Goal.GoalStatus.ACTIVE);
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setStreak(0);
        goal.setLongestStreak(0);
        goal.setCreatedAt(Instant.now());
        goal.setUpdatedAt(Instant.now());

        log.info("Goal created: userId={}, name={}, target={}", userId, goal.getName(), goal.getTargetAmount());
        return goalRepository.save(goal);
    }

    /**
     * Get all goals for a user.
     */
    public List<Goal> getUserGoals(String userId) {
        return goalRepository.findByUserId(userId);
    }

    /**
     * Get a specific goal (with ownership check).
     */
    public Goal getGoal(String goalId, String userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        if (!goal.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Goal not found");
        }
        return goal;
    }

    /**
     * Daily check-in — log how much the user saved today.
     * If already checked in today, updates the existing check-in.
     */
    public GoalCheckIn checkIn(String goalId, String userId, BigDecimal amount, String note) {
        Goal goal = getGoal(goalId, userId);
        String today = LocalDate.now().toString();
        List<GoalCheckIn> todayCheckIns = checkInRepository.findByGoalIdAndDate(goalId, today);

        GoalCheckIn checkIn;
        if (!todayCheckIns.isEmpty()) {
            // Update existing check-in for today
            checkIn = todayCheckIns.get(0);
            BigDecimal oldAmount = checkIn.getAmount();
            goal.setCurrentAmount(goal.getCurrentAmount().subtract(oldAmount).add(amount));
            checkIn.setAmount(amount);
            checkIn.setNote(note);
            checkIn.setCumulativeTotal(goal.getCurrentAmount());
        } else {
            // New check-in
            goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
            checkIn = GoalCheckIn.builder()
                    .goalId(goalId)
                    .userId(userId)
                    .date(today)
                    .amount(amount)
                    .note(note)
                    .cumulativeTotal(goal.getCurrentAmount())
                    .build();

            // Update streak
            String yesterday = LocalDate.now().minusDays(1).toString();
            boolean hadYesterday = !checkInRepository.findByGoalIdAndDate(goalId, yesterday).isEmpty();
            goal.setStreak(hadYesterday ? goal.getStreak() + 1 : 1);
            if (goal.getStreak() > goal.getLongestStreak()) {
                goal.setLongestStreak(goal.getStreak());
            }
        }

        // Check completion
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(Goal.GoalStatus.COMPLETED);
            goal.setCompletedAt(Instant.now());
        }

        goal.setUpdatedAt(Instant.now());
        goalRepository.save(goal);
        checkIn.setCreatedAt(Instant.now());
        return checkInRepository.save(checkIn);
    }

    /**
     * Get check-in history for a goal.
     */
    public List<GoalCheckIn> getCheckIns(String goalId, String userId) {
        getGoal(goalId, userId); // Verify ownership
        return checkInRepository.findByGoalIdOrderByDateDesc(goalId);
    }

    /**
     * Delete a goal and all its check-ins.
     */
    public void deleteGoal(String goalId, String userId) {
        Goal goal = getGoal(goalId, userId);
        List<GoalCheckIn> checkIns = checkInRepository.findByGoalIdOrderByDateDesc(goalId);
        checkInRepository.deleteAll(checkIns);
        goalRepository.delete(goal);
        log.info("Goal deleted: goalId={}, userId={}", goalId, userId);
    }
}
