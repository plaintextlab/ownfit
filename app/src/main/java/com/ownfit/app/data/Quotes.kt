package com.ownfit.app.data

import java.time.LocalDate

object Quotes {
    private val list = listOf(
        "Small steps every day add up to big results.",
        "You don't have to be extreme, just consistent.",
        "The only bad workout is the one that didn't happen.",
        "Motivation gets you started. Habit keeps you going.",
        "Strong is built one rep at a time.",
        "Your body can do it. Convince your mind.",
        "Progress, not perfection.",
        "Sweat now, shine later.",
        "Ten minutes is better than zero minutes.",
        "Discipline is choosing what you want most over what you want now.",
        "Take care of your body. It's the only place you have to live.",
        "Rest is part of the plan, not a break from it.",
        "Breathe in strength, breathe out doubt.",
        "Every rep is a vote for the person you want to become.",
        "Show up for yourself today.",
        "Hard days build strong people.",
        "Earn your rest. Then take it.",
        "Move because you love your body, not because you hate it.",
        "A one-hour workout is 4% of your day.",
        "You're one workout away from a better mood.",
        "Don't count the days. Make the days count.",
        "Fall in love with taking care of yourself.",
        "Slow progress is still progress.",
        "Stronger than yesterday.",
        "Be proud of how far you've come.",
        "The best project you'll ever work on is you.",
        "Calm mind, strong body.",
        "What seems impossible today becomes your warm-up tomorrow.",
        "Consistency beats intensity.",
        "Do it for the future you.",
    )

    fun today(): String = list[(LocalDate.now().toEpochDay() % list.size).toInt()]
    fun random(): String = list.random()
}
