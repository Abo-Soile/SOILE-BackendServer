[![Build Status](https://github.com/Abo-Soile/SOILE-BackendServer/workflows/CI-CD/badge.svg)](https://github.com/Abo-Soile/SOILE-BackendServer/workflows/CI-CD/badge.svg)
[![codecov](https://codecov.io/gh/Abo-Soile/SOILE-BackendServer/branch/main/graph/badge.svg?token=1H9DR2CR7S)](https://codecov.io/gh/Abo-Soile/SOILE-BackendServer)
[![License (MIT)](https://shields.io/badge/license-MIT-informational)](https://shields.io/badge/license-MIT-informational)

# SOILE Backend Server

This is the package for the SOILE Backend Server
The Server is based on [Vertx 4](https://vertx.io)
The API is located at [swaggerHub](https://app.swaggerhub.com/apis/THOMASPFAU/SoileAPI/1.0.0) and also available in the project.

# Concepts:

## Versioning

The SOILE platform aims at facilitating versioning and avoiding data loss, when changing objects. Under the hood, git is used to reflect changes and allow recovery of previous versions. This is reflected on the platform in that any use of an Object requires indication of the ID (Name) and the Version, in order to allow the system to retrieve the correct information. This also makes it more robust to changes, such that any editing of an object will not lead to another object containing an instance of it to be altered. E.g. If you have a Project that contains Task A at version 1, and you now change Task A and thus create a new version 2, the project will still refer to Task A at version 1 and will use that version. This can be a bit inconvenient, when updating, since you might need to update several versions, however it guaratees, that if e.g. someone runs a study which contains Task A, the study is not altered when another user creates a new version of Task A for their study.

## Studies

Using the SOILE infrastructure, allows you to design your experimental studies in a flexible way combining multiple different approaches into one coherent study. A study essentially represents a Project and allows participants to perform the tasks in the underlying project.

### Projects

Projects combine multiple tasks and experiments. They allow the introduction of filters which can be thought of as crossroads in the project flow. Filters indicate which task comes next for a participant based on previously obtained data. When using filters, make sure, that the data the individual options of the filter refer to are surely present for a participant who reaches this point of the study.
You can create cycles using filters which allow you to have a participant repeat a certain task until they achieve a specific goal in the task.

### Experiments

Experiments in SOILE represent a collection of tasks or a specific flow that you expect to re-use un-altered in multiple studies. E.g. if you have a fixed set of introductory questions followed by a simple reaction test, and you expect those to be re-used in multiple projects, you can combine them in an experiment allowing you to reduce the amount of elements in your projects, making it potentially clearer when building it.

### Filters

Filters as mentioned above act as crossroads in projects in soile. They check previous data and assign the next task based on that information. Make sure, that the different options in a filter are mutually exclusive since no guarantee is given which option is selected otherwise (the implementation should not matter for your study!)

### Randomizers

Randomizers are similar to filters except that they assign participants according to a specific randomization scheme. This can be completely random, or it can be blocks, where each participant gets assigned to one.

### Tasks

Tasks are the basic units in all SOILE Projects. A task represents an individual set of instructions, one exercise one questionnaire etc. They can also be more complex, but the assumption of SOILE is that one task is one unit. A task is run entirely on the participants machine (with only resources being loaded from the server). If you have any time-critical experiments and you are not using the Soile experiment language, psychoJS or another established library for your task, make sure, that the data is loaded before the task is started.

### Instantiation

SOILE at many places makes a distinction between a Task and an instance of a task. E.g. a Task has some code, additional files etc but is essentially independent of any other task. However, a task instance can access data that was explicitly stored in previous tasks can define outputs it generates which can be used in filters, or can define data that other tasks can use. This means that when setting up a task in a project or experiment, you need to ensure that the data that the task claims to export (set up as outputs or as persistent data) are actually created by the task.

# Dependencies

The backend depends on several [vertx](https://vertx.io) packages: - core - web - config - auth-mongo - mongo-client - auth-jdbc - web-openapi - auth-jwt
Logging is achieved via log4j
Formula parsing for filters is achieved using [exp4j](https://github.com/fasseg/exp4j)
It also makes use of a couple of [apache commons](https://commons.apache.org) libraries: - [commons-lang3](https://commons.apache.org/proper/commons-lang/) - [commons-math3](https://commons.apache.org/proper/commons-math/) - [commons-io](https://commons.apache.org/proper/commons-io/)
