// Job definition to test PostgreSQL connector against different PostgreSQL versions

matrixJob('connector-debezium-db2-matrix-test') {

    displayName('Debezium DB2 Connector Test Matrix')
    description('Executes tests for DB2 Connector')
    label('Slave')

    axes {
        label("Node", "Slave")
    }

    properties {
        githubProjectUrl('https://github.com/debezium/debezium-connector-db2')
    }

    parameters {
        stringParam('MAIL_TO', 'debezium-qe@redhat.com')
        stringParam('REPOSITORY', 'https://github.com/debezium/debezium-connector-db2', 'Repository from which connector is built')
        stringParam('BRANCH', 'main', 'A branch/tag from which the connector is built')
        stringParam('SOURCE_URL', "", "URL to productised sources")
        booleanParam('PRODUCT_BUILD', false, 'Is this a productised build?')
    }

    triggers {
        cron('H 04 * * *')
    }

    wrappers {
        preBuildCleanup()

        timeout {
            noActivity(1200)
        }
    }

    publishers {
        archiveJunit('**/target/surefire-reports/*.xml')
        archiveJunit('**/target/failsafe-reports/*.xml')
        mailer('$MAIL_TO', false, true)
    }

    logRotator {
        daysToKeep(7)
        numToKeep(10)
    }

    steps {
        shell('''
# Ensure WS cleaup
ls -A1 | xargs rm -rf

# Retrieve sources
if [ "$PRODUCT_BUILD" == true ] ; then
    export MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
    PROFILE_PROD="-Ppnc"
    curl -OJs $SOURCE_URL && unzip debezium-*-src.zip
    pushd debezium-*-src
    pushd debezium-connector-db2-*
else
    git clone $REPOSITORY .
    git checkout $BRANCH
fi

# Run connector tests
mvn clean install -U -s $HOME/.m2/settings-snapshots.xml -am -fae \
    -Dmaven.test.failure.ignore=true \
    -Dtest.argline="-Ddebezium.test.records.waittime=5" \
    -Dinsecure.repositories=WARN \
    $PROFILE_PROD 
''')
    }
}
