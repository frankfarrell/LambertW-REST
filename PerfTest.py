__author__ = 'Frank'

import pycurl
import json
from  datetime import datetime, timezone, timedelta
import random


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

try:
    for i in range(10000052, 11000000):
        offset = random.randint(1, 10000)
        time = datetime.now(timezone.utc).astimezone() - timedelta(hours=offset)
        put_order(i, time.isoformat())
except ValueError:
    print("Max number is %s.",  i)

