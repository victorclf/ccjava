#!/usr/bin/env python
import sys
import argparse
import urllib
import os
import getpass
import codecs
import shutil
import time
import random

import github

DEFAULT_OUTPUT_PATH = 'pullRequests'
MAX_DOWNLOAD_RETRIES = 3
PULL_REQUEST_IDS_FILE = 'pull-req-ids.log'
CREDENTIALS_FILE = '.pullRequestMinerrc'


def parseArgs():
	parser = argparse.ArgumentParser(
		description='Tool for extracting pull requests from GitHub. Downloads all open pull requests of the specified projects.')
	parser.add_argument("projectsFullName", nargs="+", help="github projects that you want to extract the pull requests from (format: owner/projectName)")
	parser.add_argument('-a', '--authenticate', action="store_true", help="ask for username and password to get a higher rate limit from Github API")
	parser.add_argument('-o', '--outputPath', action="store", nargs="?", default=DEFAULT_OUTPUT_PATH, help="output path. If not specified will default to: " + DEFAULT_OUTPUT_PATH)
	parser.add_argument('-f', '--forceRedownload', action="store_true", help="force redownload of all files even if they are already present in the local filesystem")
	parser.add_argument('-c', '--closedPullRequests', action="store_true", help="also download pull requests that were closed")
	parser.add_argument('-n', '--numPullRequestsToDownload', action="store", type=int, default=sys.maxsize, help="number of pull requests to download")
	parser.add_argument('-i', '--onlyGetPullRequestIds', action="store_true", help="only generate files with the pull request ids of each project, don't download the pull requests")
	args = parser.parse_args()
	numProjects = len(args.projectsFullName)
	args.numPullRequestsPerProjectToDownload = args.numPullRequestsToDownload / numProjects
	return args


def getCredentials():
	credentialsFilePath = os.path.join(os.path.expanduser("~"), CREDENTIALS_FILE)
	if os.path.exists(credentialsFilePath):
		with open(credentialsFilePath) as fin:
			l = fin.readline()
			username, password = l.strip().split(':')
	else:
		defaultUsername = os.getlogin()
		username = raw_input('username (default: %s): ' % defaultUsername)
		if not username.strip():
			username = defaultUsername

		password = getpass.getpass('password: ')
	return username, password


def getCurrentTimestamp():
	return time.strftime("%y%m%d%H%M%S")


def downloadPullRequestData(pull, pullRequestPath):
	wholePatchPath = os.path.join(pullRequestPath, '%d.patch' % pull.number)
	urllib.urlretrieve(pull.patch_url, wholePatchPath)

	for f in pull.get_files():
		#Don't download files which were deleted.
		if (f.additions > 0 or f.changes > 0) and f.patch:
			filePath = os.path.join(pullRequestPath, f.filename)
			if not os.path.exists(os.path.dirname(filePath)):
				os.makedirs(os.path.dirname(filePath))

			urllib.urlretrieve(f.raw_url, filePath)
			patchPath = filePath + ".patch"
			with codecs.open(patchPath, "w", encoding="UTF-8") as fout:
				fout.write(f.patch)
				fout.write('\n')


def minePullRequest(pull, pullRequestPath, skipExistingData=True):
	if skipExistingData and os.path.exists(pullRequestPath):
		print 'Pull request #%d was downloaded before. Skipping...' % pull.number
	else:
		print 'Mining pull request #%d' % pull.number
		# Save files in a temporary folder. In case there is a problem during the download, this lets us know what we need to redownload.
		incompletePullRequestPath = pullRequestPath + '-incomplete-' + getCurrentTimestamp()
		os.makedirs(incompletePullRequestPath)

		retries = 0
		while 1:
			try:
				downloadPullRequestData(pull, incompletePullRequestPath)
				break
			except:
				print 'ERROR: Failed to mine pull request #%d' % pull.number
				if retries >= MAX_DOWNLOAD_RETRIES:
					raise
				else:
					print "Retrying..."
					retries += 1

		if not skipExistingData and os.path.exists(pullRequestPath):
			shutil.rmtree(pullRequestPath, True)
		os.rename(incompletePullRequestPath, pullRequestPath)


def containsJavaCode(pullRequestPath):
	for root, dirs, files in os.walk(pullRequestPath):
		for f in files:
			if f.endswith(".java"):
				return True

	return False


def getPullRequestIds(pulls, repoPullRequestsPath):
	idsPath = os.path.join(repoPullRequestsPath, PULL_REQUEST_IDS_FILE)
	
	pullRequestIds = []
	if os.path.exists(idsPath):
		with open(idsPath) as fin:
			pullRequestIds = [int(l.strip()) for l in fin.readlines()]
	else :	
		nPullRequests = 0
		for p in pulls:
			pullRequestIds.append(p.number)
			nPullRequests += 1
			if nPullRequests % 100 == 0:
				print '.',
				sys.stdout.flush()
		random.shuffle(pullRequestIds)
		print '\nFound %d pull requests' % len(pullRequestIds)
		
		with open(idsPath, 'w') as fout:
			for pid in pullRequestIds:
				fout.write(str(pid) + os.linesep)
	
	return pullRequestIds


def minePullRequests(repo, repoPullRequestsPath, includeClosedPullRequests, numPullRequestsToDownload, skipExistingData=True, onlyGetPullRequestIds=False):
	pulls = repo.get_pulls(state="all") if includeClosedPullRequests else repo.get_pulls()
	
	pullRequestIds = getPullRequestIds(pulls, repoPullRequestsPath)
	if onlyGetPullRequestIds:
		return
	
	print 'Random sampling %d pull requests' % numPullRequestsToDownload
	nDownloaded = 0
	while pullRequestIds and nDownloaded < numPullRequestsToDownload:
		pid = pullRequestIds.pop(0)
		p = repo.get_pull(pid)
		
		pullRequestPath = os.path.join(repoPullRequestsPath, str(p.number))
		minePullRequest(p, pullRequestPath, skipExistingData)

		if not containsJavaCode(pullRequestPath):
			print 'Ignored pull request #%d because it doesn\'t have any java code' % p.number
			shutil.rmtree(pullRequestPath, True)
		else:
			nDownloaded += 1
	print 'Downloaded %d pull requests' % nDownloaded

		


def main():
	args = parseArgs()

	if args.authenticate:
		username, password = getCredentials()
		gh = github.Github(username, password)
	else:
		gh = github.Github()

	for fullName in args.projectsFullName:
		print 'Mining %s' % fullName

		try:
			repo = gh.get_repo(fullName)
		except github.UnknownObjectException:
			print 'ERROR: repository "%s" was not found' % fullName
			continue

		repoPullRequestsPath = os.path.join(args.outputPath, repo.full_name)
		if not os.path.exists(repoPullRequestsPath):
			os.makedirs(repoPullRequestsPath)

		minePullRequests(repo, repoPullRequestsPath, args.closedPullRequests, args.numPullRequestsPerProjectToDownload, not args.forceRedownload, args.onlyGetPullRequestIds)

if __name__ == '__main__':
	main()
