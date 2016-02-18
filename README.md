[![Build Status](https://travis-ci.org/frankfarrell/SNOWBALL-MAGIC-19851014.svg?branch=master)](https://travis-ci.org/frankfarrell/SNOWBALL-MAGIC-19851014)

# Intro

* RESTful Priority queue backed by Redis.

* The queue has 4 levels of priority, where priority is a function of time, eg nlogn
  1. Management -> Always has highest priority, ranked by time amongst themselves
  2. VIP -> Rank is Max(4, 2*n*log(n)) where n is duration in queue
  3. Priority -> Rank is Max(3, n*log(n)) where n is duration in queue
  4. Normal -> Rank is n where n is duration in queue

# How to Use : REST Endpoints

* Try it out here http://franksorder.herokuapp.com/swagger-ui.html

* GET /workorder
  * Returns all work orders in Queue, with Id, Time of Insertion, Duration in Queue, Current Position in Queue and Type of Order

* GET /workorder/{id} 404,
  * Returns work order with this id, with Time of Insertion, Duration in Queue, Current Position in Queue and Type of Order

* DELETE /workorder/{id}
  * Deletes work order with this id from the queue
  * This is an atomic operation, ie two concurrent clients cannot remove and pop the same work order

* PUT /workorder
  * Takes an Work Order with id and timestamp, which is placed in the queue.
  * NB: Timestamp, and hence queue position is determined by the client.
  * This is an atomic operation, ie two concurrent clients cannot push work orders with the same id

* POST /workorder
  * Pops the top priority work order from the queue.
  * NB: this was not implemented as GET as this is not an idempotent request
  * This is an atomic operation, ie two concurrent clients will not receive same work order

* POST /workorder/statistics
  * Takes an object {filters : \["normal"\], statistics : \["averageWait"\]}, where filter filters on types. Empty filters gives average of all items
  * This is implemented as POST because it is snapshot aggregation of the state of the system, rather than a representation of actual state

# How to Build and Run

* To run quickly with an embedded redis :
  * gradle bootRun -Dspring.redis.embedded=true -Dspring.redis.port=6999
  * Launch ui at localhost:8080/swagger-ui.html

* To run with a local standalone running redis:
  * gradle bootRun (Optional Argument -Dspring.redis.port=6379)
  * Launch ui at localhost:8080/swagger-ui.html

* On each commit there is a build and deploy using Travis-Ci to Heroku:
  * https://travis-ci.org/frankfarrell/SNOWBALL-MAGIC-19851014
  * http://franksorder.herokuapp.com/swagger-ui.html

# Tech Stack

* Spring Boot
* Redisson Client for Redis

# Implementation

1. Each Work Order type is backed by a Redis ScoredSortedSet of Longs, using Redisson library
  1. On push, the score is calculated as seconds since the Unix Epoch
  2. On pop, the lowest scoring member of each queue (eg, the longest queued member) is ranked according to the priority functions above
  3. These operations (and remove) are made thread safe by using a single threaded executor pool, since Redis does not provide (nicely) transactions on non-atomic operations.
2. On Get all, all queues are merged and ranked according to priority function
3. On Put and Get of single item, the current position in queue is calculated (this is a function of time)
  1. This uses the Redis ZRANGEBYSCORE command. 
  2. We calculate the rank priority function for the current item
  3. We then calculate the inverse priority function for other queues, eg what duration in queue would give an equal rank
  4. For nlogn operations this uses the LambertW function: https://en.wikipedia.org/wiki/Lambert_W_function#Example_4

#  Tests:

* WebControllerMVC
* Unit
* Spring Integration with embedded redis TODO
* Performance Testing TODO

# TODO

* Distributed Read Write Locking on queues, eg for multiple Service instances
* UI Client that simulates push and pop requests and show status of the queue. Order, wait time etc.
* More queue statistics