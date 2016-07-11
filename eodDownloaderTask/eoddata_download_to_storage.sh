#! /bin/bash

apt-get -y install python-pip
apt-get -y install python-dev
pip install --upgrade six
pip install google-api-python-client
pip install gcloud
pip install --upgrade google-api-python-client
pip install --upgrade gcloud
gsutil cp gs://optionfusion_com/scripts/eoddata_download_to_storage.py /etc
python /etc/eoddata_download_to_storage.py
poweroff

