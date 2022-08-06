#!/usr/bin/env python

import sys
import os
import simplejson
import collections
import gzip
import datetime

import requests

from PullRequest import PullRequest

GITHUB_ARCHIVE_URL_TEMPLATE = 'http://data.githubarchive.org/%s.json.gz'
TMP_DIR = '/tmp'


def run(cmd):
	print cmd
	os.system(cmd)


def download(url, saveDir=TMP_DIR, overwrite=False):
	path = os.path.join(TMP_DIR, os.path.basename(url))
	if os.path.exists(path) and not overwrite:
		pass
	else:
		run('aria2c -x10 -d%s %s 1> /dev/null 2>&1' % (saveDir, url))
	return path


def json2obj(data):
	def _json_object_hook(d): 
		return collections.namedtuple('X', d.keys(), rename=True)(*d.values())
	return simplejson.loads(data, object_hook=_json_object_hook)
	

def extractEventsFromJSON(filePath):
	events = []
	with gzip.open(filePath, 'rb') as fin:
		for line in fin:
			#~ event = json2obj(line) # Slow performance...
			e = simplejson.loads(line)
			events.append(e)
	return events


def extractPullRequestsFromEvents(pullDB, events):
	for e in events:
		if e['type'] == 'PullRequestEvent':
			repoId = e['payload']['pull_request']['base']['repo']['full_name']
			pullId = e['payload']['number']
			pullKey = (repoId, pullId)
			language = e['payload']['pull_request']['base']['repo']['language']
			language = language.lower() if language is not None else language
			action = e['payload']['action']
			author = e['payload']['pull_request']['user']['login']
			#email = e['payload']['pull_request']['user'].get('email')
			createdAt = e['payload']['pull_request']['created_at']
			updatedAt = e['payload']['pull_request']['updated_at']
			
			if action == 'opened' and language == 'java' and pullKey not in pullDB:
				pull = PullRequest(repoId, pullId, author, createdAt, updatedAt)
				pullDB[pullKey] = pull


def _strLinesDict(d):
	lines = []
	for k, v in d.iteritems():
		lines.append(k)
		if type(v) is dict:
			lines += ['  ' + l for l in _strLinesDict(v)]
		else:
			lines[-1] = k + ': ' + str(v)
	return lines

	
def strDict(d):
	return '\n'.join(_strLinesDict(d))


def getPullRequestsCreatedAt(pullDB, t=(datetime.datetime.utcnow() - datetime.timedelta(hours=3))):
	assert type(pullDB) is dict
	url = GITHUB_ARCHIVE_URL_TEMPLATE % t.strftime('%Y-%m-%d-%H')
	filePath = download(url)
	print 'Parsing events from', filePath
	events = extractEventsFromJSON(filePath)
	print 'Parsing pull requests from %d events in %s' % (len(events), filePath)
	extractPullRequestsFromEvents(pullDB, events)
	os.remove(filePath)


def main():
	pullDB = {}
	getPullRequestsCreatedAt(pullDB)
	for p in pulls:
		print p
	
	
if __name__ == '__main__':
	main()


