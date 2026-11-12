# dpLab

dpLab is an independent Android companion workspace. It is not the official DeepSeek app and does not redistribute or patch DeepSeek's proprietary APK.

## v1.0.5
- The first tab loads the official DeepSeek web chat at https://chat.deepseek.com/ inside Android WebView. Sign in with your own DeepSeek account; conversations are handled by DeepSeek's own website, not by a dpLab API server.
- No DeepSeek API key is required by dpLab.
- WebView cookies are retained to keep the official website session on the device.
- Web chat file selection is passed to Android's document picker.
- Built-in and user-created prompt skills are stored locally.
- Create text files in a folder selected through Android Storage Access Framework.
- GitHub connector: encrypt a Personal Access Token with Android Keystore, check the account, list repositories, and create private Gists.

## Privacy and account
The official DeepSeek website receives requests, sign-in information and chat content directly according to its own service and privacy terms. dpLab does not proxy or inspect the chat traffic. Avoid entering your account credentials anywhere except the official DeepSeek domain. DeepSeek's terms say users under 18 should read the terms with a legal guardian and use the service only with guardian consent.

## GitHub permissions
Use a fine-grained token with Metadata read-only for repository listing and Gists read/write for private Gist creation. Never paste tokens into chat or commit them to this repository.

## Build and signing
GitHub Actions builds a debug-signed APK and publishes it as a GitHub Release asset. This is installable for testing. GitHub-hosted runners may generate different debug signing keys on later builds, so updating over an earlier build may require uninstalling it first.
