package me.pandras.color_names

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroup
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import java.awt.datatransfer.StringSelection

/**
 * An action to copy the name of a color to the clipboard in a specified format.
 * This action is added to notifications related to color names.
 *
 * @property project The current project context, may be used to scope the notification.
 * @property caseType The format of the color name to copy ("camelCase" or "snake_case").
 */
class CopyColorNameAction(private val project: Project?, private val caseType: String) : NotificationAction("$caseType") {
    /**
     * Executes the action of copying the color name to the clipboard in the specified case format.
     * Also triggers a notification indicating the action was performed.
     *
     * @param e The event related to the action.
     * @param notification The notification object associated with this action.
     */
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        // Get the color name from the notification content
        val content = notification.content
        val colorName = content.split(" is ")[1]  // Assuming the color name is after " is "

        // Apply the chosen case conversion
        val convertedColorName = when (caseType) {
            "camelCase" -> colorName.camelCase()
            "snake_case" -> colorName.snakeCase()
            else -> colorName  // Default to original case if not recognized
        }

        // Copy the converted color name to the clipboard
        val stringSelection = StringSelection(convertedColorName)
        val clipboard = CopyPasteManager.getInstance()
        clipboard.setContents(stringSelection)
        notification.expire()

        val notificationGroup = NotificationGroup.findRegisteredGroup("ColorNamesNotificationGroup")

        if(notificationGroup != null){
            val customIconPath = "/color-names.svg" // Path to your icon
            val customIcon = IconLoader.getIcon(customIconPath, this::class.java)
            val copyNotification = Notification(
                notificationGroup!!.displayId,
                "Color Name Copied",
                "Color name \"$convertedColorName\" has been copied to clipboard.",
                NotificationType.INFORMATION
            ).apply {
                setIcon(customIcon)
            }
            Notifications.Bus.notify(copyNotification)
        }
    }

    /**
     * Copies the given text to the system clipboard.
     *
     * @param text The text to be copied to the clipboard.
     */
    private fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        val clipboard = CopyPasteManager.getInstance()
        clipboard.setContents(stringSelection)
    }

    /**
     * Converts a string to camelCase format.
     *
     * @return The camelCase formatted string.
     */
    fun String.camelCase(): String {
        val words = this.split(" ")
        val camelCaseWords = words.mapIndexed { index, word ->
            if (index == 0) word.toLowerCase()
            else word.capitalize()
        }
        return camelCaseWords.joinToString("")
    }

    /**
     * Converts a string to snake_case format.
     *
     * @return The snake_case formatted string.
     */
    fun String.snakeCase(): String {
        return this.replace(" ", "_").toLowerCase()
    }
}
