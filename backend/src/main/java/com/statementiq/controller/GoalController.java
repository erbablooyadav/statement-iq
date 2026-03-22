package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.model.Goal;
import com.statementiq.model.GoalCheckIn;
import com.statementiq.model.User;
import com.statementiq.service.domain.GoalService;
import com.statementiq.service.domain.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;
    private final UserService userService;

    public GoalController(GoalService goalService, UserService userService) {
        this.goalService = goalService;
        this.userService = userService;
    }

    /**
     * POST /goals — Create a new saving goal.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Goal>> createGoal(
            @RequestBody Goal goal, Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        Goal created = goalService.createGoal(user.getId(), goal, user);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    /**
     * GET /goals — List all goals.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Goal>>> listGoals(Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        List<Goal> goals = goalService.getUserGoals(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(goals));
    }

    /**
     * GET /goals/{id} — Get goal detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Goal>> getGoal(
            @PathVariable String id, Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        Goal goal = goalService.getGoal(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(goal));
    }

    /**
     * POST /goals/{id}/checkins — Daily check-in.
     */
    @PostMapping("/{id}/checkins")
    public ResponseEntity<ApiResponse<GoalCheckIn>> checkIn(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String note = body.getOrDefault("note", "").toString();
        GoalCheckIn checkIn = goalService.checkIn(id, user.getId(), amount, note);
        return ResponseEntity.ok(ApiResponse.ok(checkIn));
    }

    /**
     * GET /goals/{id}/checkins — Get check-in history.
     */
    @GetMapping("/{id}/checkins")
    public ResponseEntity<ApiResponse<List<GoalCheckIn>>> getCheckIns(
            @PathVariable String id, Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        List<GoalCheckIn> checkIns = goalService.getCheckIns(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(checkIns));
    }

    /**
     * DELETE /goals/{id} — Delete a goal.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteGoal(
            @PathVariable String id, Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        goalService.deleteGoal(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Goal deleted"));
    }
}
