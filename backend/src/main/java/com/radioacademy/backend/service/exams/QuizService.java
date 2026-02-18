package com.radioacademy.backend.service.exams;

import com.radioacademy.backend.dto.exams.*;
import com.radioacademy.backend.entity.*;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.ModuleRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.repository.exams.QuizAttemptRepository;
import com.radioacademy.backend.repository.exams.QuizRepository;
import com.radioacademy.backend.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final ModuleRepository moduleRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    
    
    
    @Transactional
    public QuizDTO createQuiz(QuizDTO request) {
        Module module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new IllegalArgumentException("Module not found"));

        Quiz quiz = quizRepository.findByModuleId(module.getId())
                .orElse(new Quiz());

        if (quiz.getId() == null) {
            quiz.setModule(module);
        }
        quiz.setTitle(request.title());

        List<Question> currentQuestions = quiz.getQuestions();
        Set<UUID> incomingQuestionIds = new HashSet<>();

        if (request.questions() != null) {
            for (QuestionDTO qDto : request.questions()) {
                Question question;

                
                Optional<Question> existingQ = currentQuestions.stream()
                        .filter(q -> q.getId() != null && q.getId().equals(qDto.id()))
                        .findFirst();

                if (existingQ.isPresent()) {
                    
                    question = existingQ.get();
                    question.setActive(true);
                    incomingQuestionIds.add(question.getId());
                } else {
                    
                    question = new Question();
                    question.setActive(true);
                    question.setQuiz(quiz); 
                    
                    
                    currentQuestions.add(question);
                }

                
                question.setContent(qDto.content());
                question.setPoints(qDto.points());

                
                mergeOptions(question, qDto.options());
            }
        }

        
        currentQuestions.forEach(q -> {
            if (q.getId() != null && !incomingQuestionIds.contains(q.getId())) {
                q.setActive(false);
            }
        });

        
        Quiz savedQuiz = quizRepository.save(quiz);

        
        
        return mapQuizToDTO(savedQuiz);
    }

    private void mergeOptions(Question question, List<OptionDTO> optionDTOs) {
        if (optionDTOs == null)
            return;

        List<Option> currentOptions = question.getOptions();
        Set<UUID> incomingOptionIds = new HashSet<>();

        for (OptionDTO oDto : optionDTOs) {
            Option option;

            Optional<Option> existingOpt = currentOptions.stream()
                    .filter(o -> o.getId() != null && o.getId().equals(oDto.id()))
                    .findFirst();

            if (existingOpt.isPresent()) {
                option = existingOpt.get();
                option.setActive(true);
                incomingOptionIds.add(option.getId());
            } else {
                option = new Option();
                option.setActive(true);
                option.setQuestion(question); 
                currentOptions.add(option); 
            }

            option.setText(oDto.text());
            option.setCorrect(oDto.isCorrect());
        }

        currentOptions.forEach(o -> {
            if (o.getId() != null && !incomingOptionIds.contains(o.getId())) {
                o.setActive(false);
            }
        });
    }

    
    
    
    @Transactional(readOnly = true)
    public QuizDTO getQuizById(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        
        List<Question> allQuestions = quiz.getQuestions().stream()
                .filter(Question::isActive)
                .collect(Collectors.toList()); 

        
        Collections.shuffle(allQuestions);

        
        int limit = Math.min(allQuestions.size(), 50);
        List<Question> randomQuestions = allQuestions.subList(0, limit);

        
        List<QuestionDTO> questionDTOs = randomQuestions.stream().map(q -> new QuestionDTO(
                q.getId(),
                q.getContent(),
                q.getOptions().stream()
                        .filter(Option::isActive)
                        .map(o -> new OptionDTO(o.getId(), o.getText(), false)) 
                        .toList(),
                q.getPoints())).toList();

        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getModule().getId(), questionDTOs);
    }

    
    
    
    @Transactional
    public QuizResultDTO submitQuiz(QuizSubmissionDTO submission, CustomUserDetails userDetails) {
        Quiz quiz = quizRepository.findById(submission.quizId()).orElseThrow();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(userRepository.getReferenceById(userDetails.getId()));
        attempt.setQuiz(quiz);
        attempt.setCompletedAt(LocalDateTime.now());

        
        Map<UUID, Boolean> questionResults = new HashMap<>();
        Map<UUID, UUID> correctOptions = new HashMap<>();

        int correctCount = 0;
        int totalQuestions = 0;

        
        for (Map.Entry<UUID, UUID> entry : submission.answers().entrySet()) {
            UUID questionId = entry.getKey();
            UUID optionId = entry.getValue();

            
            
            Question question = quiz.getQuestions().stream()
                    .filter(q -> q.getId().equals(questionId))
                    .findFirst()
                    .orElse(null);

            if (question == null)
                continue; 
            totalQuestions++;

            QuizAnswer answer = new QuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            Option selectedOpt = question.getOptions().stream()
                    .filter(o -> o.getId().equals(optionId))
                    .findFirst()
                    .orElse(null);

            
            boolean isCorrect = selectedOpt != null && selectedOpt.isCorrect();
            answer.setSelectedOption(selectedOpt);
            answer.setCorrect(isCorrect);

            attempt.getAnswers().add(answer);

            
            questionResults.put(questionId, isCorrect);
            if (isCorrect)
                correctCount++;

            
            question.getOptions().stream()
                    .filter(Option::isCorrect)
                    .findFirst()
                    .ifPresent(opt -> correctOptions.put(questionId, opt.getId()));
        }

        
        double score = totalQuestions > 0 ? ((double) correctCount / totalQuestions) * 100 : 0;
        attempt.setScore(score);
        attempt.setPassed(score >= 50.0);

        attemptRepository.save(attempt);

        return new QuizResultDTO(score, attempt.isPassed(), questionResults, correctOptions);
    }

    
    
    
    @Transactional(readOnly = true)
    public QuizDTO getSmartFailedQuiz(UUID quizId, CustomUserDetails userDetails) {
        UUID userId = userDetails.getId();
        Quiz quiz = quizRepository.findById(quizId).orElseThrow();

        
        List<UUID> failedIds = attemptRepository.findFailedQuestionIds(userRepository.getReferenceById(userId), quizId);

        if (failedIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No tienes fallos pendientes.");
        }

        
        List<QuestionDTO> failedQuestions = quiz.getQuestions().stream()
                .filter(q -> failedIds.contains(q.getId()))
                .map(q -> new QuestionDTO(
                        q.getId(),
                        q.getContent(),
                        q.getOptions().stream()
                                .filter(Option::isActive)
                                .map(o -> new OptionDTO(o.getId(), o.getText(), false))
                                .toList(),
                        q.getPoints()))
                .toList();

        return new QuizDTO(
                quizId,
                "Repaso: " + quiz.getTitle(),
                quiz.getModule().getId(),
                failedQuestions);
    }

    
    
    
    @Transactional(readOnly = true)
    public Optional<QuizDTO> getQuizByModuleId(UUID moduleId) {
        return quizRepository.findByModuleId(moduleId).map(this::mapQuizToDTO);
    }

    
    private QuizDTO mapQuizToDTO(Quiz quiz) {
        List<QuestionDTO> questions = quiz.getQuestions().stream()
                .filter(Question::isActive)
                .map(q -> new QuestionDTO(
                        q.getId(),
                        q.getContent(),
                        q.getOptions().stream()
                                .filter(Option::isActive)
                                .map(o -> new OptionDTO(o.getId(), o.getText(), o.isCorrect())) 
                                                                                                
                                                                                                
                                .toList(),
                        q.getPoints()))
                .toList();

        return new QuizDTO(quiz.getId(), quiz.getTitle(), quiz.getModule().getId(), questions);
    }
}