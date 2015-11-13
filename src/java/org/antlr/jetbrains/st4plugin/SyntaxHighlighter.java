package org.antlr.jetbrains.st4plugin;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

public abstract class SyntaxHighlighter {
	public static final TextAttributesKey[] NO_ATTR =
		new TextAttributesKey[] {TextAttributesKey.createTextAttributesKey("NO_ATTR")};

	final EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();

	protected Editor editor;
	protected int startIndex;

	public SyntaxHighlighter(Editor editor, int startIndex) {
		this.editor = editor;
		this.startIndex = startIndex;
	}

	/** Return a stream of Tokens covering all input characters in text. If your
	 *  ANTLR grammar skips stuff like whitespace rather than using HIDDEN
	 *  channel, that stuff won't be highlighted.
	 */
	@NotNull
	public abstract CommonTokenStream tokenize(String text);

	public void highlightEmbedded(Token t) {
	}

	public ParserRuleContext parse(CommonTokenStream tokens) { return null; }

	public void parseEmbedded(Token t) {
	}

	/** Override this if you want to do some simple context-sensitive
	 *  highlighting. For general case, a real parser should be used.
	 *  Most of the time, just override {@link #getAttributesKey(int)}.
	 */
	@NotNull
	public TextAttributesKey[] getAttributesKey(CommonTokenStream tokens, int tokenIndex) {
		Token t = tokens.get(tokenIndex);
		return getAttributesKey(t.getType());
	}

	/** Implement this to map token types to attributes.
	 *  @return Key for attribute or NO_ATTR if no highlighting desired.
	 */
	@NotNull
	public abstract TextAttributesKey[] getAttributesKey(int tokenType);

	public boolean isEmbeddedLanguageToken(int tokenType) { return false; }

	public void highlight() {
		MarkupModel markupModel = getEditor().getMarkupModel();
		markupModel.removeAllHighlighters();

		String text = getEditor().getDocument().getText();
		highlight(text, 0);
	}

	/** Highlight tokens in editor and assume all token char indexes
	 *  are relative to start. This is useful for embedded
	 *  languages.
	 */
	public void highlight(String text, int start) {
		CommonTokenStream tokens = tokenize(text);
//		System.out.println(tokens.getTokens());
		for (int i=0; i<tokens.size(); i++) {
			Token t = tokens.get(i);
			if ( t.getType()!=Token.EOF ) {
				if ( isEmbeddedLanguageToken(t.getType()) ) {
					highlightEmbedded(t);
				}
				else {
					highlightToken(t, start);
				}
			}
		}

		ParserRuleContext tree = parse(tokens);
		highlightTree(tree, tokens);
	}

	protected void highlightToken(Token t, int start) {
		TextAttributesKey[] keys = getAttributesKey(t.getType());
		if ( keys!=NO_ATTR ) {
			MarkupModel markupModel = getEditor().getMarkupModel();
			final EditorColorsScheme scheme =
				editorColorsManager.getScheme(EditorColorsScheme.DEFAULT_SCHEME_NAME);
			TextAttributes attr = merge(scheme, keys);
			RangeHighlighter h =
				markupModel.addRangeHighlighter(
					start+t.getStartIndex(),
					start+t.getStopIndex()+1,
					HighlighterLayer.SYNTAX, // layer
					attr,
					HighlighterTargetArea.EXACT_RANGE);
//			h.putUserData(getHighlightCategoryKey(), t);
		}
	}

	protected void highlightTree(ParserRuleContext tree, CommonTokenStream tokens) {
	}

	public Editor getEditor() {
		return editor;
	}

	public static TextAttributes merge(EditorColorsScheme scheme, TextAttributesKey[] keys) {
		TextAttributes attrs = new TextAttributes();
		for (TextAttributesKey key : keys) {
			TextAttributes a = scheme.getAttributes(key);
			if (a != null) {
				attrs = TextAttributes.merge(attrs, a);
			}
		}
		return attrs;
	}

	public static void removeHighlighters(Editor editor, Key<?> key, int start, int stop) {
		// Remove anything with user data accessible via key
		System.out.println("removeHighlighters: "+start+","+stop);
		MarkupModel markupModel = editor.getMarkupModel();
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
			if ( r.getUserData(key)!=null && r.getStartOffset()>=start && r.getEndOffset()<=stop+1 ) {
				markupModel.removeHighlighter(r);
			}
		}
	}
}