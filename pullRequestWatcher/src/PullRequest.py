#!/usr/bin/env python
import re
import requests

import Util

class PullRequest(object):
	PARTITION_JSON_REGEX = re.compile(r'Partitions \(NTP: ([0-9]+) / TP: ([0-9]+)\)') #Partitions (NTP: XXX / TP: YYY)
	
	def __init__(self, repoId, pullId, author, createdAt, updatedAt, analyzedAt=None, ntp=None, tp=None):
		self.repoId = repoId
		self.pullId = pullId
		self.author = author
		self.createdAt = createdAt
		self.updatedAt = updatedAt
		self.analyzedAt = analyzedAt
		self.ntp = ntp
		self.tp = tp
		
		if analyzedAt is None or ntp is None or tp is None:
			self._downloadPartitionResults()
	
	@property
	def gitHubUrl(self):
		return 'https://github.com/%s/pull/%d/' % (self.repoId, self.pullId)
	
	@property
	def jccUrl(self):
		return 'http://www.jclusterchanges.com/pulls/%s/%d/' % (self.repoId, self.pullId)
	
	@property
	def interesting(self):	
		return 2 <= self.ntp <= 5
	
	def __str__(self):
		return '%s %d %s %s %s %s %d %d' % (self.repoId, self.pullId, self.author, self.createdAt, self.updatedAt, self.analyzedAt, self.ntp, self.tp)
	
	@classmethod
	def fromString(cls, s):
		p = s.split()
		repoId = p[0]
		pullId = int(p[1])
		author = p[2]
		createdAt = p[3]
		updatedAt = p[4]
		analyzedAt = p[5]
		ntp = int(p[6])
		tp = int(p[7])
		return cls(repoId, pullId, author, createdAt, updatedAt, analyzedAt, ntp, tp)
		
	def __hash__(self):
		return hash((self.repoId, self.pullId))

	def __eq__(self, other):
		return (self.repoId, self.pullId) == (other.repoId, other.pullId)

	def __ne__(self, other):
		return not(self == other)
		
	def _downloadPartitionResults(self):
		partitionsUrl = self.jccUrl + 'partitions/'
		print '  GET', partitionsUrl
		r = requests.get(partitionsUrl)
		print '    status', r.status_code
		
		self.ntp = -1
		self.tp = -1
		try:
			if 200 <= r.status_code <= 299:
				m = self.PARTITION_JSON_REGEX.match(r.json()['text'])
				self.ntp = int(m.group(1))
				self.tp = int(m.group(2))
				print '    NTP: %d / TP: %d' % (self.ntp, self.tp)
			else:
				print '    FAIL!!!'
		except Exception, e:
			print e
		self.analyzedAt = Util.getCurrentUTCDateTime().isoformat()
