package com.example.service;



import com.example.model.Question;

import java.util.List;

public class QuestionService implements IQuestionService{

    private List<Question> questions;

    public QuestionService(List<Question> questions){
        this.questions=questions;
    }

    @Override
    public List<Question> getQuestions(){
        return questions;
    }
}