package com.cryptroot.core.dialogue;

/**
 * An immutable data carrier for a single line in a {@link ConversationScript}.
 *
 * @param speaker the {@link Speaker} who delivers this line
 * @param text the text to display in the message box
 */
public record MessageLine(Speaker speaker, String text) {}
