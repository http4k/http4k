#!/usr/bin/env python

import re
import glob
import shutil
import requests
import os


def pull_content(uri):
    uri = uri \
        .replace('github.com', 'raw.githubusercontent.com') \
        .replace('/blob/', '/')

    print('Fetching ' + uri)
    response = requests.get(uri)
    if response.status_code > 299:
        raise Exception('Server responded with ' + str(response.status_code))
    return '```\n' + response.text + '\n```'


if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.realpath(__file__))
    project_root = script_dir + '/../'
    working_dir = script_dir + '/../build/docs-website'

    shutil.rmtree(working_dir, ignore_errors=True)
    os.makedirs(working_dir, exist_ok=True)
    shutil.copytree(project_root + '/src/docs', working_dir + '/docs')
    shutil.copy(project_root + '/CONTRIBUTING.md', working_dir + '/docs/contributing/index.md')
    shutil.copy(project_root + '/CODE_OF_CONDUCT.md', working_dir + '/docs/code-of-conduct/index.md')
    shutil.copy(project_root + '/CHANGELOG.md', working_dir + '/docs/changelog/index.md')
    shutil.copy(project_root + '/README.md', working_dir + '/docs/index.md')

    pages = [f for f in glob.glob(working_dir + "/**/*.md", recursive=True)]
    for page in pages:
        print('Processing ' + page)
        with open(page, 'r', encoding="utf-8") as file:
            page_contents = file.read()
        page_contents = re.sub(r"<script src=\"https://gist-it\.appspot\.com/(.*)\"></script>",
                               lambda m: pull_content(m.group(1)),
                               page_contents)
        with open(page, 'w', encoding="utf-8") as file:
            file.write(page_contents)
