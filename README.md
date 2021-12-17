# Issue Cache 
## Motivation
Suppose that you are a part of a team working on a web app that connects to external systems. We'll use Atlassian Jira as an example here, as it's the system our company works with, but we'll keep it simple for the sake of this task. The only thing you need to know about Jira is that it stores issues, which have fields (such as Summary, Description or Assignee). You can assume that users are only interested in 4-5 fields.

The UI of this supposed app is centered around a table that shows Jira issues along with their fields, and this table updates its contents in real time as the issues change. The app can be used by many users simultaneously, and they all can be viewing the same table or several tables. However, the number of issues stored in the system should be much higher than the number of users. Also, since a table updates in real time, the users tend to keep these tables open for a long time. So you can assume that users open and close tables infrequently.

 You have been tasked with implementing the core of that system - the cache that stores the values of these fields, once they have been downloaded from Jira. Many developers from your team will be using this cache, so be careful in implementing it.

## The task
Your task is to design, implement and unit test an issue cache that holds field values of Jira issues.
The cache is live: it notifies its users when issues change.
To receive notifications, users subscribe to updates on a set of issues.
When issues change, all users subscribed to changed issues receive new field values.
Since loading issue from Jira takes time, you are expected to return values stored in the cache immediately, if they are already stored in the cache.

An issue is uniquely identified by a number, and a field is uniquely identified by a String.

The list of fields is fixed at cache instance creation.
Field values are loaded from Jira using the loader provided at cache instance creation,
which has the following interface: given issue IDs and field IDs,
fetch the specified issues from Jira and return values of the specified fields for these issues.

To receive notifications when issues are updated, the cache subscribes to the change tracker.
The change tracker can only tell when an issue is changed, it doesn't know which fields have changed.

Cache users call the cache from only one thread.
Change tracker notifications happen on the same thread.

## The code
This project contains starter code: the interfaces and the scaffolding needed for the testing code.
Your task will be to write your code inside the project.

## Assessment
When assessing the result of your work, we will first check if the specification is implemented correctly. We'll also look at performance characteristics of your code and internal design, as well as code quality and usability. We will also check the design of your test cases.

A note on comments: you don’t need to comment everything! Leave only comments that provide considerable help in reading the code. Comments must be in English.

## Time
This task should take no more than 6-8 hours netto.
