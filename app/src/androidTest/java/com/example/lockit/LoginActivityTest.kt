package com.example.lockit

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class   LoginActivityTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<LoginActivity> = ActivityScenarioRule(LoginActivity::class.java)


    /**
     * @author:Pedro e Dupas
     */
    @Test
    fun testLogin() {
        // Digita email
        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"), closeSoftKeyboard())

        // Digita a senha
        onView(withId(R.id.etPassword))
            .perform(typeText("password"), closeSoftKeyboard())

        // Click lno botão login
        onView(withId(R.id.btnLogin))
            .perform(click())

        // Ve se o mapa da  activity locker aparece
        onView(withId(R.id.btnMap))
            .check(matches(isDisplayed()))
    }
}
