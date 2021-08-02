YouTrack-Idea-Plugin
======================
![](http://jb.gg/badges/official-flat-square.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This plugin brings Jetbrains YouTrack bugtracker features to Intellij IDE family.

The latest released version can be found at [JetBrains IDE plugin repository](https://plugins.jetbrains.com/plugin/8215) or installed directly from an IDE itself.

Looking to report a bug or suggest a feature? Feel free to [file a YouTrack issue](https://youtrack.jetbrains.com/newIssue?project=JT&c=State%20Open&c=Subsystem%20IDE%20plugin) about it.

[![](http://img.youtube.com/vi/gGVEwlZpvj8/0.jpg)](http://www.youtube.com/watch?v=gGVEwlZpvj8)

**Compatible IDEs:** IntelliJ IDE family 2017.1+

**Compatible YouTrack versions:** 5.2+

## Installation and Configuration

This plugin relies on a long-standing task management plugin and it's context management. It will be installed automatically once YouTrack plugin is installed. Goland IDE is known to have [some issues](https://github.com/jk1/youtrack-idea-plugin/issues/66) with that though. Having YouTrack plugin installed from a plugin repository  proceed as follows:

1. Navigate to Settings dialog -> Tools -> Tasks -> Servers
2. Configure connection to your YouTrack instance

That's it, plugin is now ready to work.

## Features

#### Command execution

Among the other context types task management plugin supports bug tracker issues. When an issue from YouTrack as set as an active task it's possible to execute YouTrack command against the issue. Hit Ctrl+Shift+Y of use Tools menu to invoke a command window.

Please refer to [YouTrack command guide](https://confluence.jetbrains.com/display/YTD65/Quick+Start+Guide.+Using+Command+Window) for the general command execution rules.

#### Code navigation

When clicking a stacktrace in YouTrack the plugin is able to open the corresponding file in IDE. This feature is somewhat similar to what TeamCity plugin offers, although current implementation doesn't perform VCS lookup to find the best match for source code changing over time.
This feature requires local ports to be opened, so you may want to configure your firewall to allow connections to 127.0.0.1:63330..63339.

#### Issue view

Plugin displays issue list matching the query set in server settings. When an issue is selected, issue preview appears with wiki markup rendered.

#### Notifications

With YouTrack 2018.1+ plugin can show issue tracker notifications in IDE. To enable notifications server wide one should have 'REST API' notification channel enabled on Global Settings page in YouTrack.
To disable notifications in IDE navigate to File > Appearance & Behavior > Notifications and switch the corresponding category to 'No Balloon'.

### Time Tracking

The plugin can capture user activity and create work items for the current active task in an automated or semi-automated mode.

#### Autolinks

Issue IDs are turned into navigable links in VCS history and Java code comments.

## Plugin logs

If you think there's something wrong going on with the plugin try checking plugin logs.
To enable debug logging proceed as follows:

1. Navigate to 'Help' IDE menu and select 'Debug Log Settings...'
2. Add com.github.jk1 category to the list
3. In 'Help' menu select 'Show Log in File Manager' to explore logged data

## Plugin API

One can use [YouTrack plugin API](https://github.com/jk1/youtrack-idea-plugin/blob/master/api/src/main/java/com/github/jk1/ytplugin/YouTrackPluginApi.java) in other plugins as follows:

```project.getComponent(YouTrackPluginApi.class)```

Separate plugin api jar file is included in the plugin to be used as a compile time dependency.

## Development

Plugin requires JDK 8 installed for development.
To get an Idea project open ```build.gradle``` file with Intellij Idea and follow import instructions.

Handy development tasks:

- ```./gradlew assemble``` to build plugin in build/distributions folder

- ```./gradlew tests``` to run all tests

- ```./gradlew runIdea``` to start a copy of IntelliJ IDEA with YouTrack plugin enabled

Pull requests are most welcome.


