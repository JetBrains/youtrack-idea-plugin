package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.logger
import java.util.concurrent.TimeUnit


class TimeTracker() {

    var issueId: String = "Default"
    private var time: String = ""
    private var comment: String = ""

    private var startTime: Long = 0
    var isRunning = false

    fun stop(): String {
        return if (isRunning){
            logger.debug("Time tracking stopped")
            time = formatTimePeriod((System.currentTimeMillis() - startTime))
            isRunning = false
            time
        }
        else{
            logger.debug("Time tracking was not recorded")
            "0m"
        }
    }

    fun start(){
        if (!isRunning) {
            logger.debug("Time tracking started")
            startTime = System.currentTimeMillis()
            isRunning = true
        }
    }

    private fun formatTimePeriod(timeInMilSec: Long): String {
        var timeInMilliSec = timeInMilSec
        require(timeInMilliSec >= 0) { "0m" }

        var days = TimeUnit.MILLISECONDS.toDays(timeInMilliSec)
        timeInMilliSec -= TimeUnit.DAYS.toMillis(days)

        val weeks = days / 7
        days %=  7

        val hours = TimeUnit.MILLISECONDS.toHours(timeInMilliSec)
        timeInMilliSec -= TimeUnit.HOURS.toMillis(hours)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMilliSec)
        timeInMilliSec -= TimeUnit.MINUTES.toMillis(minutes)

        val sb = StringBuilder(64)
        if (weeks > 0)
            sb.append(weeks).append("w ")
        if (days > 0)
            sb.append(days).append("d ")
        if (hours > 0)
            sb.append(hours).append("h ")
        if (minutes > 0)
            sb.append(minutes).append("m ")

        if (sb.isEmpty())
            return "0m"

        return sb.toString()
    }

    fun getRecordedTime() = time

    fun getComment() = comment

}


