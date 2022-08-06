from sopel.module import *
from sopel.tools._events import events
import os
import time

THROTTLE_JOIN_INTERVAL = 10
THROTTLE_CHANNELS = '#elasticsearch,#kotlin,#vertx,#prestodb,#storm-user,#apache-kafka,#druid-dev,#deeplearning4j,#graylog,#hibernate-dev,#wildfly-dev,#wildfly,#hazelcast,#flink,#rxjava,#spring,#spring,#junit,#dropwizard-user,#gradle,#idea-users,#eclipseche,#neo4j,#alluxio,##closure-tools,#netty,#openhab,#rstudio,#processing,#orientdb,#gocd,#swagger,#grpc,#crate,#spring,#jmonkeyengine,#undertow,#gitblit,#jackson,#hive,#liveray,#tomcat,#apache-camel,#opengrok,#jetty,#openfire,#osmand,#vaadin,#drools,#fabric8,##pentaho,#mesos,#infinispan,#keycloak,#blueflood,#libgdx,#square,#dropwizard'

@interval(THROTTLE_JOIN_INTERVAL)
def joinChannel(bot):
	global THROTTLE_CHANNELS
	if type(THROTTLE_CHANNELS) is str:
		THROTTLE_CHANNELS = THROTTLE_CHANNELS.split(',')
	if THROTTLE_CHANNELS:
		bot.join(THROTTLE_CHANNELS.pop())
	
	
