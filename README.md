[![Build Status](https://travis-ci.org/frankfarrell/SNOWBALL-MAGIC-19851014.svg?branch=master)](https://travis-ci.org/frankfarrell/SNOWBALL-MAGIC-19851014)

# Intro

* RESTful Priority queue backed by Redis.
* The queue has 4 levels of priority, where priority is a function of time, eg nlogn

# How to Use : REST Endpoints

Try it out here swagger-ui.html

*GET /workorder
 Returns all work orders in Queue, with Id, Time of Insertion, Duration in Queue, Current Position in Queue and Type of Order

*GET /workorder/{id} 404,
Returns work order with this id, with Time of Insertion, Duration in Queue, Current Position in Queue and Type of Order

*DELETE /workorder/{id}
Deletes work order with this id from the queue

*PUT
/workorder
Takes an Work Order with id and timestamp, which is placed in the queue.
NB: Timestamp, and hence queue position is determined by the client.

*POST
/workorder
Pops the top priority work order from the queue.
NB, this was not implemented as GET as this is not an idempotent request

*POST
/workorder/statistics
Takes an object {filters : [], }
This is implemented as POST because it is snapshot aggregation of the state of the system

# How to Build and Run

* To run quickly with an embedded redis :
gradle bootRun --embedded=true --port=6999
Launch ui at localhost:8080/swagger-ui.html

* To run with a locally running redis:
gradle bootRun [Optional Argument--port=6379]

* On each commit there is a build and deploy using Travis-Ci to Heroku:
https://travis-ci.org/frankfarrell/SNOWBALL-MAGIC-19851014
http://franksorder.herokuapp.com/swagger-ui.html

# Tech Stack

* Spring Boot
* Redisson Client for redis

# Implementation

* TODO

#  Tests:
* WebControllerMVC
* Unit
* Spring Integration with embedded redis TODO
* Performance Testing TODO

# TODO
* Distributed Read Write Locking on queues
* UI Client that simulates push and pop requests and show status of the queue. Order, wait time etc.
* More queue statistics