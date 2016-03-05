import os
import shutil
import tempfile
import urlparse
import zipfile
from contextlib import closing
from datetime import datetime, timedelta

import httplib2
import requests
from gcloud import storage
from googleapiclient import discovery
from oauth2client.client import GoogleCredentials
from requests.auth import HTTPBasicAuth
import google.appengine.ext
from google.appengine.ext import taskqueue

OPTION_FUSION_API = "option-fusion-api"

_client = None
_tempdir = tempfile.mkdtemp(prefix='zipstorage')


def download_zip_file(filename):
    download_url = get_download_url(filename)
    print "Downloading " + download_url

    with closing(requests.get(download_url, stream=True, auth=get_auth())) as r:
        if r.status_code != 200:
            print "Status: %d" % r.status_code
            return

        with open(get_local_filepath(filename), 'wb') as f:
            for chunk in r.iter_content(64):
                f.write(chunk)

        print r
        print "Wrote %d bytes to %s" % (os.path.getsize(f.name), f)
        return f.name


def download_files():
    for i in range(0, 7):
        filetime = datetime.now() - timedelta(days=i)

        if filetime.isoweekday() >= 6:
            continue

        if file_for_day_exists(filetime):
            continue

        filename = get_zip_filename(filetime)
        print filename
        downloaded_file = download_zip_file(filename)

        if downloaded_file is None:
            continue

        extract_file_to_storage(downloaded_file)


def extract_file_to_storage(file_name_with_path):
    with open(file_name_with_path) as f:
        z = zipfile.ZipFile(f)
        extractedFilenames = z.namelist()
        z.extractall(_tempdir)

    for extracted in extractedFilenames:
        raw_filename = os.path.join(_tempdir, extracted)
        zip_filename = raw_filename + ".zip"
        with zipfile.ZipFile(zip_filename, 'w') as myzip:
            myzip.write(raw_filename, extracted, compress_type=zipfile.ZIP_DEFLATED)

        print "File %s size %d (%d)" % (raw_filename, os.path.getsize(raw_filename), os.path.getsize(zip_filename))

        with open(zip_filename, 'r') as myzip:
            print "Writing file " + zip_filename
            new_gs_blob(extracted + ".zip").upload_from_file(myzip)


def file_for_day_exists(dayOfData):
    blob = get_gs_blob(get_options_csv_filename(dayOfData))
    return blob is not None


def get_options_csv_filename(dayOfData):
    return dayOfData.strftime("options_%Y%m%d.csv.zip")


def get_zip_filename(dayOfData):
    return dayOfData.strftime("options_%Y%m%d.zip")


def get_auth():
    return HTTPBasicAuth(os.getenv("OPTIONSDATA_USERNAME", "tsombrero"), os.getenv("OPTIONSDATA_PASSWORD", "password"))


def get_download_url(filename):
    base = os.getenv("OPTIONSDATA_BASEURL", "http://www.deltaneutral.com/dailydata/dbupdate/")
    return urlparse.urljoin(base, filename)


def get_local_filepath(filename):
    return os.path.join(_tempdir, filename)


def get_gs_blob(filename):
    return get_gs_bucket().get_blob("csv/" + filename)


def new_gs_blob(filename):
    return get_gs_bucket().blob("csv/" + filename)


def get_gs_bucket():
    return _client.get_bucket(os.getenv("OPTIONSDATA_BUCKET_NAME", "optionfusion_com"))

def shut_down_instance():
    credentials = GoogleCredentials.get_application_default()
    compute = discovery.build('compute', 'v1', credentials=credentials)
    compute.instances().stop(project=OPTION_FUSION_API, zone="us-central1-b", instance="instance-3").execute()

def tickle_eoddata_processor():
    credentials = GoogleCredentials.get_application_default()
    http = httplib2.Http()
    http = credentials.authorize(http)
    api_root = 'https://option-fusion-api.appspot.com/admin?DAYS_TO_SEARCH=5'
    http.request(uri=api_root,
                 headers={'Content-Type': 'application/json; charset=UTF-8'})

def tickle_eoddata_processor_taskqueue():
    credentials = GoogleCredentials.get_application_default()
    taskqueue

def main():
    print "starting..."
    global _client
    _client = storage.Client(project=OPTION_FUSION_API)
    download_files()
    print "tickling servlet..."
    tickle_eoddata_processor()
    print "cleaning up..."
    shutil.rmtree(_tempdir)
    print "shutting down..."
    shut_down_instance()
    print "done"


if __name__ == '__main__':
    main()
