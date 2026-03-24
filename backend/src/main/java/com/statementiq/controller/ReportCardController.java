package com.statementiq.controller;

import com.statementiq.dto.ApiResponse;
import com.statementiq.model.ReportCard;
import com.statementiq.model.User;
import com.statementiq.repository.ReportCardRepository;
import com.statementiq.service.domain.ReportCardService;
import com.statementiq.service.domain.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/report-cards")
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final ReportCardRepository reportCardRepository;
    private final UserService userService;

    public ReportCardController(ReportCardService reportCardService,
                                ReportCardRepository reportCardRepository,
                                UserService userService) {
        this.reportCardService = reportCardService;
        this.reportCardRepository = reportCardRepository;
        this.userService = userService;
    }

    /**
     * GET /report-cards — list all report cards for user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReportCard>>> getAllCards(Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        List<ReportCard> cards = reportCardRepository.findByUserIdOrderByMonthDesc(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(cards));
    }

    /**
     * POST /report-cards/generate — generate / refresh report card for current month.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ReportCard>> generateCard(Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        ReportCard card = reportCardService.generateForCurrentMonth(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(card));
    }
}
