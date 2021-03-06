package main

import (
	"bytes"
	"javaimports/scripts/fixtures"
	"strings"
	"testing"

	"golang.org/x/net/html"
	"golang.org/x/net/html/atom"
)

func stringRepresentation(n *html.Node) string {
	var buf bytes.Buffer
	html.Render(&buf, n)
	return buf.String()
}

func genHtml(t *testing.T, raw string) *html.Node {
	node, err := html.Parse(strings.NewReader(raw))
	if err != nil {
		t.Fatalf("invalid html %s: %s", raw, err)
	}

	return node
}

// Assumes one node, and in the context of a table
func genHtmlForTable(t *testing.T, raw string) *html.Node {
	// The context of table is necessary in order to correctly parse "td"s and
	// "tr"s
	nodes, err := html.ParseFragment(strings.NewReader(raw), &html.Node{
		Type:     html.ElementNode,
		Data:     "table",
		DataAtom: atom.Table,
	})
	if err != nil {
		t.Fatalf("invalid html %s: %s", raw, err)
	}

	if len(nodes) != 1 {
		fragments := make([]string, 0)
		for _, node := range nodes {
			fragments = append(fragments, stringRepresentation(node))
		}
		t.Fatalf("why is there more than one node? %v", fragments)
	}

	if nodes[0].FirstChild != nodes[0].LastChild {
		t.Fatalf("why is there more than one child? %v", stringRepresentation(nodes[0]))
	}
	// Because we parse in the context of a table, this the only node is a tbody
	// that we filter
	return nodes[0].FirstChild
}

// Check that we combine text accordingly
func TestGetInnerTest(t *testing.T) {
	doc := genHtmlForTable(t, "<td><code><a>link</a>code</code></td>")
	got := getInnerText(doc)
	if got != "linkcode" {
		t.Errorf("Expected linkcode, got %s", got)
	}
}

// Check that we can identify static identifiers
func TestIsStatic(t *testing.T) {
	doc := genHtmlForTable(t, fixtures.StaticIdentifierJavadocCell)
	t.Log(stringRepresentation(doc))

	static := isStatic(doc)
	if !static {
		t.Errorf("Expected static")
	}
}

// Check that identifiers are correctly added
func TestAddIdentifier(t *testing.T) {
	doc := genHtmlForTable(t, fixtures.StaticMethodJavadocRow)
	ci := newClassInfo("")
	addIdentifier(&ci, doc)

	t.Log(ci)
	if len(ci.staticIdentifiers) != 1 {
		t.Errorf("Expected one static identifier, got %v", ci.staticIdentifiers)
	}

	if len(ci.visibleIdentifiers) != 0 {
		t.Errorf("Expected no visible identifier, got %v", ci.visibleIdentifiers)
	}

	if _, ok := ci.staticIdentifiers["builder"]; !ok {
		t.Errorf("Expected \"builder\" as identifier, but did not get it")
	}
}

func TestGetIdentifier(t *testing.T) {
	cell := genHtmlForTable(t, fixtures.MultipleLinksCell)
	identifier := getIdentifier(cell)

	if identifier != "parallelPrefix" {
		t.Errorf("Expected \"parallelPrefix\" as identifier, got %s", identifier)
	}
}

// Check that we can parse a full information page
func TestPopulateClassInfo(t *testing.T) {
	fullPage := genHtml(t, fixtures.MemberClassJavadoc)
	expectedStaticIdentifiers := map[string]bool{
		"DECLARED": true,
		"PUBLIC":   true,
	}

	expectedVisibleIdentifiers := map[string]bool{
		"getDeclaringClass": true,
		"getModifiers":      true,
		"isSynthetic":       true,
		"getName":           true,
	}

	ci := populateClassInfoFromHtml("", fullPage)

	if len(ci.visibleIdentifiers) != len(expectedVisibleIdentifiers) {
		t.Errorf("Expected size %d, got %d for visibleIdentifiers", len(expectedVisibleIdentifiers), len(ci.visibleIdentifiers))
	}

	if len(ci.staticIdentifiers) != len(expectedStaticIdentifiers) {
		t.Errorf("Expected size %d, got %d for staticIdentifiers", len(expectedStaticIdentifiers), len(ci.staticIdentifiers))
	}

	for i, _ := range ci.staticIdentifiers {
		if _, ok := expectedStaticIdentifiers[i]; !ok {
			t.Errorf("Unexpected static identifier %s", i)
		}
	}

	for i, _ := range ci.visibleIdentifiers {
		if _, ok := expectedVisibleIdentifiers[i]; !ok {
			t.Errorf("Unexpected visible identifier %s", i)
		}
	}
}

func TestGetAllClasses(t *testing.T) {
	fullPage := genHtml(t, fixtures.AllClassesJavadoc)
	expectedPrefixes := map[string]bool{
		"javax/swing/AbstractAction.html":                            true,
		"javax/lang/model/util/AbstractAnnotationValueVisitor6.html": true,
		"javax/lang/model/util/AbstractAnnotationValueVisitor7.html": true,
		"javax/lang/model/util/AbstractAnnotationValueVisitor8.html": true,
		"javax/swing/border/AbstractBorder.html":                     true,
		"javax/swing/AbstractButton.html":                            true,
		"javax/swing/AbstractCellEditor.html":                        true,
		"java/time/chrono/AbstractChronology.html":                   true,
		"java/util/AbstractCollection.html":                          true,
		"javax/swing/colorchooser/AbstractColorChooserPanel.html":    true,
		"javax/swing/text/AbstractDocument.html":                     true,
		"javax/swing/text/AbstractDocument.AttributeContext.html":    true,
	}

	prefixes := findAllClassPrefixes(fullPage)

	if len(prefixes) != len(expectedPrefixes) {
		t.Errorf("Expected size %d, got %d for prefixes", len(expectedPrefixes), len(prefixes))
	}

	for _, p := range prefixes {
		if _, ok := expectedPrefixes[p]; !ok {
			t.Errorf("Unexpected prefix %s", p)
		}
	}
}
