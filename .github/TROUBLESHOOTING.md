# Troubleshooting

## Enable and extract logs

You can enable the plugin's debug logs to help diagnose your issue. Here's how you can do it:

1. Enable the plugin's debug logs by setting the line below in <kbd>Help</kbd> > <kbd>Diagnostic Tools</kbd> > <kbd>Debug Log Settings…</kbd>:
  ```
  #com.github.warningimhack3r.intellijshadcnplugin
  ```

2. Restart your IDE, wait for your problem to occur or trigger it, and open the directory containing the `idea.log` file by going to <kbd>Help</kbd> > <kbd>Show Log in Finder</kbd>/<kbd>Show Log in Explorer</kbd>.

3. In there, you have 3 options to copy the logs:
   - Open the `idea.log` file with a text editor or a log viewer and copy the parts related to the plugin, i.e., the lines containing `intellijshadcnplugin`. You can for example use tools like `grep` to filter the logs.
   - Open the `idea.log` file with a text editor or a log viewer and copy the last 50–100 lines.
   - If you're comfortable with it, you can share the whole `idea.log` file.

4. To share it, you have a few options:
   - Share them in the issue using [Markdown code blocks](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/creating-and-highlighting-code-blocks#fenced-code-blocks)
   - Paste them in a text file you can drag and drop in the issue (or directly drag and drop the `idea.log` file)
   - Use a service like [GitHub Gist](https://gist.github.com/) or [Pastebin](https://pastebin.com/) and send the link

5. It is highly recommended to **disable the debug logs after you're done**: debug logs can slow down your IDE and take up a lot of disk space. The <kbd>Debug Log Settings…</kbd> process also enables internal plugin features that are not meant for regular use.

## Share your `components.json`/`ui.config.json`/`tsconfig.json` files

Theses files are read by the plugin to get most of the information it needs to work.  
In most cases of crashes or unexpected behavior, I'll ask you to share some of these files so I can reproduce the issue on my side.

If you don't want to share it, you're free to do so, but it may be harder for me to help you.

When you share the file(s), please do not omit any information from them, as it may be crucial to diagnosing the issue.

You can either:
- Copy the content of the file(s) and paste it/them in the issue using [Markdown code blocks](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/creating-and-highlighting-code-blocks#fenced-code-blocks)
- Paste the content in a text file you can drag and drop in the issue, or directly drag and drop the file(s)
- Use a service like [GitHub Gist](https://gist.github.com/) or [Pastebin](https://pastebin.com/) and send the link
