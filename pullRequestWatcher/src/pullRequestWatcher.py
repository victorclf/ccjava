#!/usr/bin/env python
import sys
import argparse
import os
import getpass
import datetime
import random
import re
import traceback

import github
import filelock
import requests

from PullRequest import PullRequest
from Project import Project
from Util import *
import GitHubArchiveWatcher

CREDENTIALS_FILE = '.pullRequestMinerrc'
PROJECT_LIST_FILE = 'projectList.conf'
PULL_DB_FILE = 'pull_db'
PROJECT_DB_FILE = 'project_db'
USER_DB_FILE = 'user_db'
RESULTS_HTML_FILE = 'results.html'
DEFAULT_RECENT_PULL_TIME_HOURS = 72
ONLINE_IRC_USER_LIST_PATH = '/tmp/onlinePullAuthors'
ONLINE_IRC_USER_LIST_LOCK_PATH = ONLINE_IRC_USER_LIST_PATH + '.lock'

HTML_HEAD = '<!doctype html>\n<html><head><meta charset="utf-8"><title>JCC - Pull Request Watcher</title></head><body>\n'
HTML_TAIL = '\n</body></html>'



class User(object):
	def __init__(self, login, email=''):
		self.login = login
		self.email = email
	
	def __repr__(self):
		return self.login
	
	def __str__(self):
		return '%s %s' % (self.login, self.email)
	
	@classmethod
	def fromString(cls, s):
		p = s.split()
		login = p[0]
		email = p[1] if len(p) > 1 else ''
		return cls(login, email)



def parseArgs():
	parser = argparse.ArgumentParser(
		description='Daemon that alerts when interesting pull requests are opened in GitHub projects.')
	parser.add_argument('-a', '--authenticate', action="store_true", help="ask for username and password to get a higher rate limit from Github API")
	parser.add_argument('-gha', '--github-archive', dest='ghArchive', action="store_true", help="use GitHub Archive to examine new pull requests from all Java projects instead of using predef project list")
	parser.add_argument('-emails', '--findEmails', action="store_true", help="whether to automatically try to find user emails")
	args = parser.parse_args()
	return args


def getCredentials():
	credentialsFilePath = os.path.join(os.path.expanduser("~"), CREDENTIALS_FILE)
	if os.path.exists(credentialsFilePath):
		with open(credentialsFilePath) as fin:
			l = fin.readline()
			username, password = l.strip().split(':')
	else:
		raise Exception("missing credentials file: " + credentialsFilePath)
	return username, password

		
def getProjectsToWatch():
	projectListFilePath = PROJECT_LIST_FILE
	projects = []
	if os.path.exists(PROJECT_LIST_FILE):
		with open(projectListFilePath) as fin:
			for l in fin.xreadlines():
				projects.append(l.strip().split(' ')[0])
	else:
		raise Exception("missing project list: " + projectListFilePath)
	return projects


def restoreProjectDB():
	projectDB = {}
	if os.path.exists(PROJECT_DB_FILE):
		with open(PROJECT_DB_FILE) as fin:
			for l in fin.xreadlines():
				project = Project.fromString(l)
				projectDB[project.repoId] = project
	return projectDB
	

def updateProjectDB(projectDB, projectsToWatch):
	for repoId in projectsToWatch:
		if repoId not in projectDB:
			projectDB[repoId] = Project(repoId)


def saveProjectDB(projectDB):
	with open(PROJECT_DB_FILE, 'w') as fout:
		for project in projectDB.itervalues():
			fout.write(str(project) + '\n')


def restorePullRequestDB():
	pullDB = {}
	if os.path.exists(PULL_DB_FILE):
		with open(PULL_DB_FILE) as fin:
			for l in fin.xreadlines():
				try:
					pull = PullRequest.fromString(l)
					pullDB[(pull.repoId, pull.pullId)] = pull
				except:
					print "Failed to parse line:", l
					print traceback.format_exc()
	return pullDB


def savePullRequestDB(pullDB):
	with open(PULL_DB_FILE, 'w') as fout:
		for pull in pullDB.itervalues():
			fout.write(str(pull) + '\n')


def restoreUserDB():
	userDB = {}
	if os.path.exists(USER_DB_FILE):
		with open(USER_DB_FILE) as fin:
			for l in fin.xreadlines():
				user = User.fromString(l)
				userDB[user.login] = user
	return userDB


def saveUserDB(userDB):
	with open(USER_DB_FILE, 'w') as fout:
		for user in userDB.itervalues():
			fout.write(str(user) + '\n')			


def analyzePullRequest(project, pullDB, ghPull):
	repoId = project.repoId
	pullId = ghPull.number
	pullKey = (repoId, pullId)
	print 'Examining pull %s/%d' % pullKey

	pull = pullDB.get(pullKey)
	
	if not pull or pull.updatedAt != ghPull.updated_at.isoformat():
		pull = PullRequest(repoId, pullId, ghPull.user.login, ghPull.created_at.isoformat(), ghPull.updated_at.isoformat())
		pullDB[pullKey] = pull


def analyzeProject(gh, project, pullDB, defaultPullsCreatedAfter):
	print 'Examining %s for new pull requests...' % project.repoId
	
	pullsCreatedAfter = project.lastRefreshed if project.lastRefreshed else defaultPullsCreatedAfter
	
	ghPulls = gh.search_issues('', sort='created', order='desc', type='pr', 
		repo=project.repoId, created='>=' + pullsCreatedAfter)
	
	for ghP in ghPulls:
		analyzePullRequest(project, pullDB, ghP)


def loadIRCOnlineUserList():
	onlineUsers = set()
	if os.path.isfile(ONLINE_IRC_USER_LIST_PATH):
		lock = filelock.FileLock(ONLINE_IRC_USER_LIST_LOCK_PATH)
		with lock:
			with open(ONLINE_IRC_USER_LIST_PATH) as fin:
				fin.readline() # discard header
				for line in fin.xreadlines():
					onlineUsers.add(line.strip().lower())
	else:
		print 'ERROR: Sopel IRC module not working correctly. Online IRC users detection won\'t work'
	return onlineUsers


def isUserOnlineInIRC(username):
	if not hasattr(isUserOnlineInIRC, "onlineUsers"):
		isUserOnlineInIRC.onlineUsers = loadIRCOnlineUserList()
	return username.lower() in isUserOnlineInIRC.onlineUsers


def splitPullBasedOnInterest(pullDB):
	interestingPulls = []
	notInterestingPulls = []
	for p in pullDB.itervalues():
		if p.interesting:
			interestingPulls.append(p)
		else:
			notInterestingPulls.append(p)
	return interestingPulls, notInterestingPulls


def countPulls(projectDB, pullDB):
	for pull in pullDB.values():
		project = projectDB[pull.repoId]
		if pull.interesting:
			project.interestingPulls = getattr(project, 'interestingPulls', 0) + 1
		else:
			project.notInterestingPulls = getattr(project, 'notInterestingPulls', 0) + 1


def generateHtmlTableForProjects(projectDB, pullDB):
	countPulls(projectDB, pullDB)
	html = ''
	html += '<table border="1" style="text-align: center;">'
	html += '<tr>'
	html += '<th>Repository</th>'
	html += '<th>Last Refreshed (UTC)</th>'
	html += '<th>Interesting Pulls</th>'
	html += '<th>Other Pulls</th>'
	html += '</tr>'
	for p in sorted(projectDB.values(), key=lambda x: getattr(x, 'interestingPulls', 0), reverse=True):
		html += '<tr>'
		html += '<td>%s</td>' % p.repoId
		html += '<td>%s</td>' % p.lastRefreshed.replace('T', ' ')
		html += '<td>%d</td>' % getattr(p, 'interestingPulls', 0)
		html += '<td>%d</td>' % getattr(p, 'notInterestingPulls', 0)
		html += '</tr>'
	html += '</table>'
	return html
	

def generateHtmlTableForPulls(pulls, emailColumn=False):
	html = ''
	html += '<table border="1" style="text-align: center;">'
	html += '<tr>'
	html += '<th>Repository</th>'
	html += '<th>Pull ID</th>'
	html += '<th>GitHub URL</th>'
	html += '<th>JCC URL</th>'
	html += '<th>Created (UTC)</th>'
	html += '<th>Updated (UTC)</th>'
	html += '<th>Analyzed (UTC)</th>'
	html += '<th>NTP</th>'
	html += '<th>TP</th>'
	html += '<th>Author</th>'
	html += '<th>Online? (IRC)</th>'
	if emailColumn:
		html += '<th>Email</th>'
	html += '</tr>'
	for p in sorted(pulls, key=lambda x : x.createdAt, reverse=True):
		html += '<tr>'
		html += '<td>%s</td>' % p.repoId
		html += '<td>%s</td>' % p.pullId
		html += '<td><a href="%s" target="_blank">GitHub</a></td>' % p.gitHubUrl
		html += '<td><a href="%s" target="_blank">JCC</a></td>' % p.jccUrl
		html += '<td>%s</td>' % p.createdAt.replace('T', ' ')
		html += '<td>%s</td>' % p.updatedAt.replace('T', ' ')
		html += '<td>%s</td>' % p.analyzedAt.replace('T', ' ')
		if p.ntp >= 0:
			html += '<td>%s</td>' % p.ntp
			html += '<td>%s</td>' % p.tp
		else:
			html += '<td style="background-color: #f00">%s</td>' % p.ntp
			html += '<td style="background-color: #f00">%s</td>' % p.tp
		html += '<td>%s</td>' % p.author
		if isUserOnlineInIRC(p.author):
			html += '<td style="background-color: #ccf">YES</td>'
		else:
			html += '<td></td>'
		if emailColumn:
			email = p.email if hasattr(p, 'email') else ''
			email = email if email else '?'
			emailCSV = ','.join((p.gitHubUrl, str(p.ntp + p.tp), p.author, email))
			html += '<td><a href="%s">%s</a></td>' % (emailCSV, email)
		html += '</tr>'
	html += '</table>'
	return html


def generateHtmlBody(appStartTime, projectDB, pullDB):
	html = ''
	html += '<h1>JCC Pull Request Watcher</h1>'
	html += '<h2>Status</h2>'
	html += '<ul>'
	if projectDB:
		html += '<li><b>Watching projects:</b> %s</li>' % str(sorted(projectDB.values(), key=lambda x: x.repoId.lower()))
	else:
		html += '<li><b>Using GitHub Archive to examine new pull requests from all Java projects</b></li>'
	html += '<li><b>Last time app run:</b> %s</li>' % appStartTime.isoformat(' ')
	html += '</ul>'
	interestingPulls, notInterestingPulls = splitPullBasedOnInterest(pullDB)
	html += '<h2>New Pull Requests with 2 <= Non-Trivial Partitions <= 5</h2>'
	html += generateHtmlTableForPulls(interestingPulls, True)
	html += '<h2>Other Recent Pull Requests</h2>'
	html += generateHtmlTableForPulls(notInterestingPulls)
	if projectDB:
		html += '<h2>Repository Statistics</h2>'
		html += generateHtmlTableForProjects(projectDB, pullDB)
	return html


def saveHtml(appStartTime, projectDB, pullDB):
	with open(RESULTS_HTML_FILE, 'w') as fout:
		fout.write(HTML_HEAD)
		fout.write(generateHtmlBody(appStartTime, projectDB, pullDB))
		fout.write(HTML_TAIL)


def getRecentPullRequestsFromPredefProjects(gh, appStartTime, projectDB, pullDB):
	defaultPullsCreatedAfter = (appStartTime - datetime.timedelta(hours=DEFAULT_RECENT_PULL_TIME_HOURS)).isoformat()
	projects = projectDB.values()
	# If projects have been refreshed at the same time in the past, refresh them at random
	random.shuffle(projects)
	# The longer since a project has been refreshed, the more priority it has.
	projects.sort(key=lambda x: x.lastRefreshed) 
	try:
		for project in projects:
			analyzeProject(gh, project, pullDB, defaultPullsCreatedAfter)
			project.lastRefreshed = appStartTime.isoformat()
	except github.GithubException, e:
		print e
		

def getRecentPullRequestsFromGitHubArchive(appStartTime, pullDB):
	try:
		#~ if pullDB:
			#~ # Incremental run: analyze pulls from events that happened 2 hours ago.
			#~ GitHubArchiveWatcher.getPullRequestsCreatedAt(pullDB)
		#~ else:
			#~ # First run: analyze pulls in the last 72 hours
			#~ for h in xrange(DEFAULT_RECENT_PULL_TIME_HOURS, -1, -1):
				#~ #We add 2 below because GitHub archive takes up to 2 hours to parse events in last hour of GitHub
				#~ t = appStartTime - datetime.timedelta(hours=h + 2)
				#~ GitHubArchiveWatcher.getPullRequestsCreatedAt(pullDB, t)
		GitHubArchiveWatcher.getPullRequestsCreatedAt(pullDB)
	except Exception, e:
		print e


def getGitHubEmail(gh, login):
	try:
		ghUser = gh.get_user(login)
		return ghUser.email
	except github.GithubException, e:
		print e
		return ''


def getCommitEmail(gh, login, ghUsername, ghPassword):
	if not hasattr(getCommitEmail, "emailRegex"):
		getCommitEmail.emailRegex = re.compile(r'"email":"([^"]*)"')
	
	email = ''	
	try:
		url = 'https://api.github.com/users/%s/events/public' % login
		r = requests.get(url, auth=(ghUsername, ghPassword))
		m = getCommitEmail.emailRegex.search(r.text)
		if m:
			email = m.group(1)
	except Exception, e:
		print e
	return email
	

def findEmail(gh, userDB, login, ghUsername, ghPassword):
	if login in userDB:
		return userDB[login].email
	
	email = ''
	ghEmail = getGitHubEmail(gh, login)
	if ghEmail:
		email = ghEmail
	else:
		commitEmail = getCommitEmail(gh, login, ghUsername, ghPassword)
		if commitEmail:
			email = commitEmail
	
	userDB[login] = User(login, email)
	return email


def findEmails(gh, userDB, pullDB, ghUsername, ghPassword):
	for p in pullDB.itervalues():
		if p.interesting:
			p.email = findEmail(gh, userDB, p.author, ghUsername, ghPassword)


def main():
	args = parseArgs()

	username, password = getCredentials()
	gh = github.Github(username, password)
	
	appStartTime = getCurrentUTCDateTime()
	
	projectDB = []
	pullDB = restorePullRequestDB()
	userDB = []
	
	if args.ghArchive:
		getRecentPullRequestsFromGitHubArchive(appStartTime, pullDB)
	else:
		projectDB = restoreProjectDB()
		updateProjectDB(projectDB, getProjectsToWatch())
		getRecentPullRequestsFromPredefProjects(gh, appStartTime, projectDB, pullDB)
		saveProjectDB(projectDB)
	
	savePullRequestDB(pullDB)
	
	if args.findEmails:
		print 'Finding user emails...'
		userDB = restoreUserDB()
		findEmails(gh, userDB, pullDB, username, password)
		saveUserDB(userDB)
	
	saveHtml(appStartTime, projectDB, pullDB)
		
		
if __name__ == '__main__':
	main()
