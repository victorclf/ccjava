#!/usr/bin/env python

class Project(object):
	def __init__(self, repoId, lastRefreshed=None):
		self.repoId = repoId
		self.lastRefreshed = lastRefreshed
	
	def __repr__(self):
		return self.repoId
	
	def __str__(self):
		return '%s %s' % (self.repoId, self.lastRefreshed)
	
	@classmethod
	def fromString(cls, s):
		p = s.split()
		repoId = p[0]
		lastRefreshed = p[1]
		return cls(repoId, lastRefreshed)
