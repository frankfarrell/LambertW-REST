[![Build Status](https://travis-ci.org/frankfarrell/SNOWBALL-MAGIC-19851014.svg?branch=master)](https://travis-ci.org/frankfarrell/SNOWBALL-MAGIC-19851014)

 * Endpoints
GET
/workorder
GET 
/workorder/{id} 404, 
DELETE 
/workorder/{id}
POST
/workorder/{id} 404, 409, 412

Average
Either 
GET
/workorder/average
POST 
/workorder?average=true
Why: Not getting any state of internal system, but a snapshot


* Questions 

Peristent or not?
Highly available
Performance/throughput -> 80 a week
Rest interface needs to be fast :
Cache results?

* Tech Stack

Gradle
Spring Boot

* Implementation

Workhandler mock ,with interface

Priorirty Queue Class : Backed by Some Concurrent Data Structure? 
Internally : 4 queues. LIFO for each. Use Stack.

Methods:

Insert(WorkOrder)
GetByIndex(index)
GetById(id) : returns index too
AverageTime : Mean wait time across all stacks. 
Delete(id)
Pop/Shift : pop top priority from queue -> Algorithm for determining, quick qay needed to calculate nlogn, 2nlog2n

* Simple Object

On new request: 
Id of user : 9,223,372,036,854,775,807 is the max value of signed int in 64-bit. Eg Long, 26^3-1
We need a quick qay to calculate %3, %5 https://en.wikipedia.org/wiki/Exponentiation_by_squaring

Timestamp: Current : Use Java8 Time. Handle Timezones etc
If must be unique, only one work order per id
What if order with earlier timestamp received for a given id?

 Workorder: 
 {
	 id: 
	 ISOTimestamp: 
 }
 
* Rest Details

Error codes
Spring REST Exception Handler

* Peristence

Or backed by Redis? If you include flag in start script
Reddison, persistent across failures, can be distributed
Or use rabbitMQ, pub sub mechanism : easy to plug in new work handler.

Tests: 
WebControllerMVC
Unit
Spring Integration

Use Swagger and Javadocs

Test Python Script for generating requests
UI to show results? Yo angular