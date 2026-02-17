#!/bin/bash

echo "Waiting for MongoDB services to start..."
sleep 10

until mongosh --host mongo1:27017 --eval 'quit(db.getSiblingDB("admin").adminCommand({ ping: 1 }).ok ? 0 : 2)'
do
  echo "Waiting for mongo1 to be up..."
  sleep 1
done

echo "mongo1 is up. Initiating replica set..."

mongosh --host mongo1:27017 -u $MONGO_INITDB_ROOT_USERNAME -p $MONGO_INITDB_ROOT_PASSWORD --authenticationDatabase admin <<EOF
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "mongo1:27017" },
    { _id: 1, host: "mongo2:27017" },
    { _id: 2, host: "mongo3:27017" }
  ]
});
EOF