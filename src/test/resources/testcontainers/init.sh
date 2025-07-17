#!/bin/bash

echo "### CREATE QUEUES ###"

queues="local-delivery-push-inputs.fifo local-delivery-push-actions-done local-ext-channels-outputs local-national-registries-gateway"

for qn in  $( echo $queues | tr " " "\n" ) ; do

    echo creating queue $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn

done
echo "Initialization terminated"
