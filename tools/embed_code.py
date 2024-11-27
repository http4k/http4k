#!/usr/bin/env python

import os
import requests
import shutil


def pull_content(uri):
    uri = uri \
        .replace('github.com', 'raw.githubusercontent.com') \
        .replace('/blob/', '/')

    print('Fetching ' + uri)
    response = requests.get(uri)
    if response.status_code > 299:
        raise Exception('Server responded with ' + str(response.status_code))
    return '```kotlin\n' + response.text + '\n```'

def copy_and_rename_readme_to_index(src_dir, docs_dir):
    if not os.path.exists(docs_dir):
        os.makedirs(docs_dir)

    # Walk through all directories and subdirectories in the source directory
    for root, _, files in os.walk(src_dir):
        for file in files:
            if file == "README.md" and root != ".":
                # Construct full file paths
                readme_path = os.path.join(root, file)
                relative_path = os.path.relpath(root, src_dir)
                target_dir = os.path.join(docs_dir, relative_path)

                # Ensure the target subdirectory exists
                if not os.path.exists(target_dir):
                    os.makedirs(target_dir)

                index_path = os.path.join(target_dir, "index.md")

                # Copy and rename README.md to INDEX.md
                shutil.copyfile(readme_path, index_path)
                print(f"Copied and renamed: {readme_path} -> {index_path}")


if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.realpath(__file__))
    project_root = script_dir + '/../connect'
    working_dir = script_dir + '/../build/docs-website'

    shutil.rmtree(working_dir, ignore_errors=True)
    os.makedirs(working_dir, exist_ok=True)

    copy_and_rename_readme_to_index("../tools", "src/docs/guide/reference")

    shutil.copytree(project_root + '/src/docs', working_dir + '/docs')
