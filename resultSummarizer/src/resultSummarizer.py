#!/usr/bin/env python
import argparse
import os
import sys
import re
import time
import shutil

RESULTS_FOLDER_NAME = "ccjava-results"
CSV_FILE_NAMES = ('defs.csv', 'diffRelations.csv', 'diffs.csv', 
	'partitions.csv', 'summary.csv', 'uses.csv')
CCJAVA_HOME_PATH_ENV_VAR = "CCJAVA_HOME_PATH"
CCJAVA_TOOL_PATH = "ccjava-plugin/build/eclipse/eclipse"
CCJAVA_STDOUT_LOG = 'ccjava-stdout.log'
CCJAVA_STDERR_LOG = 'ccjava-stderr.log'
CCJAVA_TOOL_ARGS = "-consoleLog -nosplash --launcher.suppressErrors -vmargs -Xms40m -Xmx1024m 1>>%s 2>>%s" % (CCJAVA_STDOUT_LOG, CCJAVA_STDERR_LOG)

class CSV(object):
	def __init__(self, header, body):
		self.header = header
		self.body = body
		
	def appendBody(self, other):
		assert self.header == other.header
		self.body += other.body
		
	def __str__(self):
		return self.header + ''.join(self.body)


class PullRequest(object):
	CSV_HEADER_APPEND = 'projectName,pullRequestId'

	def __init__(self, projectName, pullRequestId, path):
		self.projectName = projectName
		self.pullRequestId = pullRequestId
		self.path = path
		self.csv = {}

	@classmethod
	def fromPath(cls, path):
		splitPath = path.split(os.sep)
		projectName = '/'.join((splitPath[-3], splitPath[-2]))
		pullRequestId = splitPath[-1]
		return cls(projectName, pullRequestId, path)
		
	def _appendCSVHeader(self, headerLine):
		return '%s,%s' % (self.CSV_HEADER_APPEND, headerLine)
		
	def _appendCSVRow(self, bodyLine):
		return '%s,%s,%s' % (self.projectName, self.pullRequestId, bodyLine)

	def _readCSVFile(self, fname):
		filePath = os.path.join(self.path, RESULTS_FOLDER_NAME, fname)
		with open(filePath) as fin:
			lines = fin.readlines()
		header = self._appendCSVHeader(lines[0])
		body = [self._appendCSVRow(l) for l in lines[1:]]
		self.csv[fname] = CSV(header,body)

	def getCSV(self, fname):
		if not fname in self.csv:
			self._readCSVFile(fname)
		return self.csv[fname]
				
	def __str__(self):
		return "%s (%s)" % (self.projectName, self.pullRequestId)


def parseArgs():
	parser = argparse.ArgumentParser(
		description='Collects data from several changesets and prints it in .csv format.')
	parser.add_argument("pullRequestsPath", help="path to the pull requests")
	parser.add_argument('-f', '--forceRerun', action="store_true", help="force rerunning ccjava on all pull requests")
	return parser.parse_args()


def isPullRequestDir(path):
	return re.match("[0-9]+$", path)


def getPullRequestsPath(basePath):
	pullRequestPaths = []
	for root, dirs, files in os.walk(basePath, topdown=True):
		pullRequestsInThisRoot = [os.path.join(root, d)
			for d in dirs if isPullRequestDir(d)]
		if pullRequestsInThisRoot:
			del dirs[:]
			
		pullRequestPaths += pullRequestsInThisRoot

	return pullRequestPaths


def generateCSV(pullRequests, fname):
	csv = None
	for p in pullRequests:
		try:
			nextCSV = p.getCSV(fname)
			if csv:
				csv.appendBody(nextCSV)
			else:
				csv = nextCSV
		except:
			print >> sys.stderr, "ERROR: couldnt read %s from %s %s" % (fname, p.projectName, p.pullRequestId)
			continue
	return csv

	
def runTimed(cmd):
	startTime = time.clock()
	os.system(cmd)
	endTime = time.clock()
	return round((endTime - startTime) * 1000000)


def getToolPath():
	homePath = os.environ[CCJAVA_HOME_PATH_ENV_VAR] if CCJAVA_HOME_PATH_ENV_VAR in os.environ else os.getcwd()
	toolPath = os.path.join(homePath, CCJAVA_TOOL_PATH)
	if not os.path.exists(toolPath):
		raise Exception("ccjava was not found at: %s . Set the environment var %s correctly." % (toolPath, CCJAVA_HOME_PATH_ENV_VAR))
	return toolPath
	
	
def writeCCJavaLogFileHeaders(cmd):
	with open(CCJAVA_STDOUT_LOG, 'a') as fout:
		fout.write(cmd + os.linesep)
		
	with open(CCJAVA_STDERR_LOG, 'a') as fout:
		fout.write(cmd + os.linesep)
	
		
def eraseTempWorkspaceFiles(basePath):
	shutil.rmtree(os.path.join(basePath, "..", "workspace"), True)


def runCCJava(pullRequest, forceRerun=False):
	if forceRerun or not os.path.exists(os.path.join(pullRequest.path, RESULTS_FOLDER_NAME, CSV_FILE_NAMES[0])):
		cmd = "%s %s %s" % (getToolPath(), pullRequest.path, CCJAVA_TOOL_ARGS)
		writeCCJavaLogFileHeaders(cmd)
		print >> sys.stderr, "Running ccjava on %s..." % (pullRequest),
		runTime = runTimed(cmd)
		print >> sys.stderr
		#print >> sys.stderr, "Took %fs" % runTime
	else:
		print >> sys.stderr, "Skipped already analyzed pull request %s" % (pullRequest)


def main():
	args = parseArgs()

	basePath = args.pullRequestsPath
	pullRequestPaths = getPullRequestsPath(basePath)
	pullRequests = [PullRequest.fromPath(prp) for prp in pullRequestPaths]
	for p in pullRequests:
		eraseTempWorkspaceFiles(basePath)
		runCCJava(p, args.forceRerun)

	for fname in CSV_FILE_NAMES:
		outputfname = "all" + fname.capitalize()
		print "Generating %s..." % outputfname
		csv = generateCSV(pullRequests, fname)
		with open(outputfname, "w") as fout:
			fout.write(str(csv))


if __name__ == '__main__':
	main()
