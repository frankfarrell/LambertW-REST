__author__ = 'Frank'

import pycurl
import json
from  datetime import datetime, timezone, timedelta
import random
import threading


try:
    # python 3
    from urllib.parse import urlencode
except ImportError:
    # python 2
    from urllib import urlencode

#curl -X PUT --header "Content-Type: application/json" --header "Accept: application/json" -d "{
#  \"id\": 1,
#  \"timeStamp\": \"2016-02-15T11:23:18+01:00\"
#}" "http://localhost:8080/workorder"


def put_order(id, timestamp="2016-02-15T11:23:18+01:00"):

    payload = {"id": id, "timeStamp" : timestamp}
    json_payload = json.dumps(payload)
    c = pycurl.Curl()
    c.setopt(c.URL, "http://localhost:8080/workorder")
    c.setopt(c.HTTPHEADER, ["Content-Type: application/json", 'Accept: application/json'])
    c.setopt(c.CUSTOMREQUEST, "PUT")
    c.setopt(c.POSTFIELDS, json_payload)
    c.perform()

    resp_code = c.getinfo(c.HTTP_CODE)
    if resp_code != 201:
        print("Error code %s.",  resp_code)
        raise ValueError('Some error here')

def pop_order():
    c = pycurl.Curl()
    c.setopt(c.CUSTOMREQUEST, "POST")
    c.setopt(c.URL, "http://localhost:8080/workorder")
    c.setopt(c.HTTPHEADER, ["Content-Type: application/json", 'Accept: application/json'])
    c.perform()

    resp_code = c.getinfo(c.HTTP_CODE)
    if resp_code != 200 and resp_code != 204:
        print("Error code %s.",  resp_code)
        raise ValueError('Some error here')

def push_loop(i):
    for i in range(1+i*10000, 20000+i*10000):
        offset = random.randint(1, 10000)
        time = datetime.now(timezone.utc).astimezone() - timedelta(hours=offset)
        put_order(i, time.isoformat())

def pop_loop():
    for i in range(1, 20000):
        pop_order()

try:
    for i in range(0, 10):
        threading.Thread(target=push_loop, args=[i]).start()
        threading.Thread(target=pop_loop).start()
except ValueError:
    print("Max number is %s.",  i)
