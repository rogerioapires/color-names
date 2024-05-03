package me.pandras.color_names

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroup
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBLabel
import com.intellij.ui.scale.JBUIScale.scale
import com.intellij.util.Alarm
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import me.pandras.color_names.CopyColorNameAction
import org.jsoup.Jsoup

/**
 * An action to fetch and display the name of a color based on its HEX code.
 * The action is triggered from the context menu within text editors.
 *
 * @property DumbAware Marks this action as performing a potentially long-running operation.
 */
class ColorNamesAction : AnAction(), DumbAware {
    /**
     * Determines the threading model for the action's execution.
     * BGT (background thread) is used here to offload work from the UI thread.
     */
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    /**
     * Called when this action is performed. This is the method that is executed when the action is triggered.
     * It processes the selected text, validates it as a hex color, fetches the color name, and displays notifications.
     *
     * @param event Provides context data about the action event including the environment it was triggered from.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText

        if (selectedText != null && isValidHex(selectedText)) {
            val hexColor = if (selectedText.startsWith("0x") && selectedText.length == 10) {
                selectedText.substring(4) // Extract the last 6 characters if hex color has "0x" prefix
            } else {
                selectedText
            }

            fetchColorName(hexColor) { colorName ->
                if (colorName != null) {
                    showColorNameNotification(event.project, colorName)
                } else {
                    showErrorNotification(event.project, "No color name found for $hexColor")
                }
            }
        } else {
            showErrorNotification(event.project, "Invalid HEX code: $selectedText")
        }
    }

    /**
     * Fetches the color name from a remote server by parsing the HTML content retrieved from the specified URL.
     * This function also handles timeouts and exceptions, ensuring the application remains responsive.
     *
     * @param hex The HEX code as a string.
     * @param callback A callback function that handles the result.
     */
    private fun fetchColorName(hex: String, callback: (String?) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val url = URL("https://www.color-name.com/hex/$hex")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 10000 // 10 seconds

                val htmlContent = connection.inputStream.bufferedReader().use { it.readText() }
                val doc = Jsoup.parse(htmlContent)
                val titleText = doc.select("title").first()?.text()  // Get content of the <title> tag
                ApplicationManager.getApplication().invokeLater {
                    callback(titleText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ApplicationManager.getApplication().invokeLater {
                    callback(null)
                }
            }
        }
    }

    /**
     * Displays a notification with the color name or an error if the name cannot be fetched.
     *
     * @param project The current project context.
     * @param titleText The text to display within the notification.
     */
    private fun showColorNameNotification(project: Project?, titleText: String?) {
        if (titleText != null) {
            val notificationGroup = NotificationGroup.findRegisteredGroup("ColorNamesNotificationGroup")
            if(notificationGroup != null){
                val customIconPath = "/color-names.svg" // Path to your icon
                val customIcon = IconLoader.getIcon(customIconPath, this::class.java)

                val notification = Notification(
                    notificationGroup!!.displayId,
                    "Color Name Found",
                    "$titleText",
                    NotificationType.INFORMATION
                ).apply {
                    setIcon(customIcon)
                    isImportant = true
                    addAction(CopyColorNameAction(project, "camelCase"))
                    addAction(CopyColorNameAction(project, "snake_case"))
                    addAction(createDismissAction("Dismiss"))
                }

                Notifications.Bus.notify(notification, project)
            }
        } else {
            showErrorNotification(project, "No color name found.")
        }
    }

    /**
     * Creates a dismiss action for notifications. When triggered, it expires the notification,
     * effectively removing it from the user interface.
     *
     * @param title The text to display on the action button, typically "Dismiss".
     * @return An instance of NotificationAction that can be added to a notification.
     */
    private fun createDismissAction(title: String): NotificationAction {
        return object : NotificationAction(title) {
            /**
             * Called when the dismiss action is triggered. This method expires the notification,
             * removing it from the user interface.
             *
             * @param e The action event that contains context about the action.
             * @param notification The notification that this action belongs to.
             */
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                notification.expire()
            }
        }
    }

    /**
     * Displays an error notification when an error occurs, such as an invalid HEX code or failure to fetch a color name.
     *
     * @param project The current project context, which can be null if not applicable.
     * @param message The error message to display.
     */
    private fun showErrorNotification(project: Project?, message: String) {
        val notification = Notification(
            "Color Names",
            "Error",
            message,
            NotificationType.ERROR
        )
        Notifications.Bus.notify(notification, project)
    }

    /**
     * Handles the visibility and enablement of this action based on the current context, such as the text selection in the editor.
     * Ensures that the action is only visible and enabled if the selected text matches the HEX color pattern.
     *
     * @param e The event that contains information about the current state of the application.
     */
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val selectionModel = editor?.selectionModel
        val selectedText = selectionModel?.selectedText

        val hexRegex = Regex("^([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8}|0x[A-Fa-f0-9]{6}|0x[A-Fa-f0-9]{8})$")

        e.presentation.isEnabledAndVisible = selectedText?.matches(hexRegex) == true
    }

    /**
     * Validates if the selected string is a valid HEX code.
     *
     * @param hex The string to validate.
     * @return Boolean indicating whether the string is a valid HEX code.
     */
    private fun isValidHex(hex: String): Boolean {
        val hexRegex = Regex("^([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8}|0x[A-Fa-f0-9]{6}|0x[A-Fa-f0-9]{8})$")
        return hex.matches(hexRegex)
    }
}
