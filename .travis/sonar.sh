#!/bin/bash
if [ "${TRAVIS_REPO_SLUG}" == "molgenis/molgenis" ] && [ "${TRAVIS_PULL_REQUEST}" == "false" ]; then
	echo "Running sonar analysis in maven..."
    ./mvnw sonar:sonar -Dsonar.login=${SONAR_TOKEN} -Dsonar.branch=${TRAVIS_BRANCH} --batch-mode --quiet
    echo "Done."
else
    echo "Skipped Sonar analysis (only runs for committed builds on the molgenis/molgenis repo)"
fi
