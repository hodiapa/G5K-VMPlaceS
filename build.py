#!/usr/bin/python
from __future__ import print_function

import logging
import glob
import os
import shutil
import optparse
import subprocess
import sys

################################################################################
# Configure logging
################################################################################
# create logger
logger = logging.getLogger('build.py')
logger.setLevel(logging.DEBUG)

# create console handler and set level to debug
ch = logging.StreamHandler()
ch.setLevel(logging.DEBUG)

# create formatter
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')

# add formatter to ch
ch.setFormatter(formatter)
# add ch to logger
logger.addHandler(ch)


################################################################################
# Check if this script should reclone and compile entropy
################################################################################
entropy_jar_name = "entropy-2.1.0-SNAPSHOT-jar-with-dependencies.jar"

def reclone_entropy():
	# Cloning the entropy project
	subprocess.call([
		"git",
		"clone",
		"https://github.com/BeyondTheClouds/Entropy.git"
	])

	# Launching mvn in the project
	os.chdir("Entropy")
	subprocess.call([
		"mvn",
		"package",
		"-DskipTests",
	])
	os.chdir("..")

	# Copy the generated jar in the lib folder
	generated_jar_name = entropy_jar_name
	shutil.copy(
		"Entropy/target/%s" %(generated_jar_name),
		"lib/%s" % (generated_jar_name)
	)

	# Removing the entropy folder
	subprocess.call([
		"rm",
		"-rf",
		"Entropy"
	])

parser = optparse.OptionParser(usage='usage: %prog [options] arguments')
parser.add_option(
	'-c',
	'--clone_entropy',
	dest='clone_entropy',
    action="store_true",
	help=("will reclone a proper entropy from github.com, "
	 "and use maven to create a clean jar (default value: False).")
)

(options, args) = parser.parse_args()

entropy_jar_may_be_found = False
if os.path.exists("lib/%s" % (entropy_jar_name)):
	entropy_jar_may_be_found = True
else:
	logger.warning("I could not found '%s'" % (entropy_jar_name))

if options.clone_entropy or not entropy_jar_may_be_found:
	logger.info("I reclone entropy from github.com")
	reclone_entropy()
else:
	logger.info(
		"I detected a potential entropy jar: %s" % (
			entropy_jar_may_be_found
		)
	)

# sys.exit(0)

################################################################################
# Create the 'fat jar' for the project
################################################################################
logger.info("Creating the experimental environment")



# # Move the jar in a fake maven repository
# fake_maven_repository_path = "mvn_repo"
# if os.path.exists(fake_maven_repository_path):
# 	logger.info("removing existing folder %s." % (fake_maven_repository_path))
# 	shutil.rmtree(fake_maven_repository_path)

# destination_folder = "%s/entropy/entropy/2.1.0-SNAPSHOT/" % (fake_maven_repository_path)
# os.makedirs(destination_folder)

# origin = "lib/%s" % (entropy_jar_path)
# destination = "%s/%s" % (destination_folder, entropy_jar_path)

# shutil.copy(origin, destination)

# Call maven to build the fat jar
logger.info("Building the fat jar")

subprocess.call(["mvn", "package"])


################################################################################
# Package the runnable jar with dependencies
################################################################################
logger.info("Packaging the project in a 'clean' archive")

# Create some folders to host files required for the execution of the project.
output_folder = "output"
if os.path.exists(output_folder):
	logger.info("removing existing folder '%s'." % (output_folder))
	shutil.rmtree(output_folder)
	os.remove("%s.tgz" % (output_folder))

logger.info("creating the folder '%s'" % (output_folder))
os.makedirs("%s/jars" % (output_folder))

# Copy jars in the 'jars' sub folder
shutil.copy(
	"target/G5K-VMPlaceS-1.0-executable.jar",
	"%s/jars/G5K-VMPlaceS-1.0-executable.jar" % (output_folder)
)

for file in os.listdir("lib"):
    if file.endswith(".jar"):
    	shutil.copy("lib/%s" % (file), "%s/jars/%s" % (output_folder, file))

# Copy launch.sh script in the output folder
shutil.copy("template/launch.sh", "%s/launch.sh" % (output_folder))
subprocess.call(["chmod", "+x", "%s/launch.sh" % (output_folder)])

# Copy set_cpu_load.sh script in the output folder
shutil.copy("set_cpu_load.sh", "%s/set_cpu_load.sh" % (output_folder))
subprocess.call(["chmod", "+x", "%s/set_cpu_load.sh" % (output_folder)])

# Creating the output.tgz archive
logger.info("creating a clean archive: '%s.tgz'" % (output_folder))
subprocess.call([
	"tar",
	"-zcvf",
	"%s.tgz" % (output_folder),
	"%s" % (output_folder)
])

# Displaying information to the end user
logger.info("The preparation of the experimental environment is finished!")
logger.info("Everything needed for launching the experiment is located in")
logger.info("the 'output.tgz' archive.")