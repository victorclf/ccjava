from sopel.module import *
from sopel.tools._events import events
import filelock
import os
import datetime

GET_ONLINE_USERS_INTERVAL_SEC = 300
USER_LIST_FILE_PATH = '/tmp/onlinePullAuthors'
USER_LIST_FILE_LOCK_PATH = USER_LIST_FILE_PATH + '.lock'

def getCurrentTime():
	return datetime.datetime.now().isoformat(' ')


@interval(GET_ONLINE_USERS_INTERVAL_SEC)
def getOnlineUsers(bot):
	lock = filelock.FileLock(USER_LIST_FILE_LOCK_PATH)
	try:
		with lock.acquire(timeout = 60):
			with open(USER_LIST_FILE_PATH, 'w') as fout:
				fout.write('// Generated at %s from channels %s\n' % (getCurrentTime(), 
					' '.join(bot.channels.keys())
					))
				for user in sorted(bot.users.keys()):
					fout.write(str(user) + '\n')
	except filelock.Timeout:
		# Assumes that lock is stale and resets it
		os.remove(USER_LIST_FILE_LOCK_PATH)

	
#~ @rule(r".*")
#~ @event(events.RPL_NAMREPLY)
#~ def onNamesReply(bot, trigger):
	#~ #print trigger.args
	#~ pass
