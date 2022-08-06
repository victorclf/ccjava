#!/usr/bin/env python
import datetime

def getCurrentUTCDateTime():
	return datetime.datetime.utcnow().replace(microsecond=0)
