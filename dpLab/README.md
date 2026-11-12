# dpLab

dpLab is an independent Android companion workspace for DeepSeek. It does not patch, replace, or redistribute the original DeepSeek APK.

## v1 features
- Prepare prompts and share them to DeepSeek through Android's share sheet.
- Built-in and user-created prompt skills stored locally.
- Create text files in a folder selected through Android Storage Access Framework.
- Read a text file only after the user selects it.
- GitHub connector: encrypt a Personal Access Token with Android Keystore, check the account, list repositories, and create private Gists.

## GitHub permissions
Use a fine-grained token with Metadata read-only for repository listing and Gists read/write for private Gist creation. Never paste tokens into chat or commit them to this repository.

## Build and signing
GitHub Actions builds a debug-signed APK and publishes it as a GitHub Release asset. This is installable for testing. GitHub-hosted runners may generate different debug signing keys on later builds, so updating over an earlier build may require uninstalling it first.

## Privacy
There is no dpLab backend and no broad storage permission. The GitHub token is encrypted locally with a non-exportable Android Keystore key. Network requests go directly to api.github.com.
