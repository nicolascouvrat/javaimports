package main

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"

	"golang.org/x/net/html"
)

const (
	javaVer          = 8
	javaSEUrlBase    = "https://docs.oracle.com/javase/%d/docs/api/%s"
	allClassesPrefix = "allclasses-noframe.html"
)

// classInfo contains a description of a class (or enum, or interface)
type classInfo struct {
	// the url fragment to combine with the base URL to access this class' info
	// page
	prefix string

	name               string
	staticIdentifiers  []string
	visibleIdentifiers []string
}

func newClassInfo(prefix string) classInfo {
	return classInfo{
		prefix:             prefix,
		staticIdentifiers:  make([]string, 0),
		visibleIdentifiers: make([]string, 0),
	}
}

func traverseAndPrint(n *html.Node) {
	fmt.Println(n.Data)
	for c := n.FirstChild; c != nil; c = c.NextSibling {
		traverseAndPrint(c)
	}
}

// getIdentifier extracts the identifier from a "class=colLast" td.
//
// td is supposed to contain at most one link, and the identifier is the text
// inside
func getIdentifier(td *html.Node) string {
	var explore func(*html.Node) string
	explore = func(n *html.Node) string {
		if n.Type == html.ElementNode && n.Data == "a" {
			if n.FirstChild != n.LastChild {
				panic("link with more than one child")
			}

			if n.FirstChild.Type != html.TextNode {
				panic("link child is not text")
			}

			return n.FirstChild.Data
		}

		var combined string
		for c := n.FirstChild; c != nil; c = c.NextSibling {
			combined += explore(c)
		}

		return combined
	}

	return explore(td)
}

func getInnerText(parent *html.Node) string {
	var explore func(*html.Node) string
	explore = func(n *html.Node) string {
		if n.Type == html.TextNode {
			return n.Data
		}

		var combined string
		for c := n.FirstChild; c != nil; c = c.NextSibling {
			combined += explore(c)
		}

		return combined
	}

	return explore(parent)
}

func isStatic(parent *html.Node) bool {
	innerText := getInnerText(parent)
	return strings.HasPrefix(innerText, "static")
}

func addIdentifier(ci *classInfo, row *html.Node) {
	var (
		identifier string
		static     bool
	)
	for n := row.FirstChild; n != nil; n = n.NextSibling {
		if !(n.Type == html.ElementNode && n.Data == "td") {
			continue
		}

		var isFirstCol, isLastCol bool
		for _, a := range n.Attr {
			if a.Key == "class" && a.Val == "colFirst" {
				isFirstCol = true
			}

			if a.Key == "class" && a.Val == "colLast" {
				isLastCol = true
			}
		}

		if isLastCol && isFirstCol {
			// We cannot handle it
			fmt.Printf("unknown cell: %v\n", n)
		}

		if isLastCol {
			identifier = getIdentifier(n)
			continue
		}

		static = isStatic(n)
	}

	if identifier != "" {
		if static {
			ci.staticIdentifiers = append(ci.staticIdentifiers, identifier)
			return
		}

		ci.visibleIdentifiers = append(ci.visibleIdentifiers, identifier)
	}
}

func populateClassInfo(ci *classInfo) error {
	raw, err := getRaw(generateJavaSEUrl(ci.prefix))
	if err != nil {
		return err
	}
	doc, err := html.Parse(strings.NewReader(string(raw)))
	if err != nil {
		return err
	}

	return populateClassInfoFromHtml(ci, doc)
}

func populateClassInfoFromHtml(ci *classInfo, doc *html.Node) error {
	visitMemberRow := func(row *html.Node) {
		addIdentifier(ci, row)
	}

	doForEachMemberRow(doc, visitMemberRow)
	return nil
}

func doForEachMemberRow(n *html.Node, callback func(*html.Node)) {
	if isMemberSummary(n) {
		doForEachBodyRow(n, callback)
		return
	}

	for c := n.FirstChild; c != nil; c = c.NextSibling {
		doForEachMemberRow(c, callback)
	}
}

func doForEachBodyRow(n *html.Node, callback func(*html.Node)) {
	if isElement(n, "tbody") {
		doForEachRow(n, callback)
		return
	}

	for c := n.FirstChild; c != nil; c = c.NextSibling {
		doForEachBodyRow(c, callback)
	}
}

func doForEachRow(n *html.Node, callback func(*html.Node)) {
	if isElement(n, "tr") {
		callback(n)
		return
	}

	for c := n.FirstChild; c != nil; c = c.NextSibling {
		doForEachRow(c, callback)
	}
}

func isMemberSummary(n *html.Node) bool {
	if !isElement(n, "table") {
		return false
	}

	class, found := getAttributeValue(n, "class")
	if !found {
		return false
	}

	return class == "memberSummary"
}

func main() {
	allClasses, err := getAllClasses()
	if err != nil {
		panic(err)
	}

	fmt.Println(len(allClasses))
}

// getAllClasses queries the java SE documentation and returns all classes
//
// The resulting classInfos will only contain their prefix, and no other
// information
func getAllClasses() ([]classInfo, error) {
	raw, err := getRaw(generateJavaSEUrl(allClassesPrefix))
	if err != nil {
		return nil, err
	}

	doc, err := html.Parse(strings.NewReader(string(raw)))
	if err != nil {
		return nil, err
	}

	classes := make([]classInfo, 0)
	for _, p := range findAllClassPrefixes(doc) {
		classes = append(classes, newClassInfo(p))
	}

	return classes, nil
}

// generateJavaSEUrl generates the URL for a given page of the java SE javadoc
// given a prefix, leveraging the fact that all pages use the same base URL
func generateJavaSEUrl(prefix string) string {
	return fmt.Sprintf(javaSEUrlBase, javaVer, prefix)
}

// getRaw returns html as a string
func getRaw(url string) ([]byte, error) {
	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()
	return ioutil.ReadAll(resp.Body)
}

func findAllClassPrefixes(doc *html.Node) []string {
	var prefixes = make([]string, 0)
	// The only "a" elements are the ones contained in the big list of all
	// available classes
	storePrefix := func(a *html.Node) {
		if prefix, found := getAttributeValue(a, "href"); found {
			prefixes = append(prefixes, prefix)
		}
	}

	visitAnchorTags(doc, storePrefix)
	return prefixes
}

func visitAnchorTags(n *html.Node, callback func(*html.Node)) {
	if isElement(n, "a") {
		callback(n)
	}

	for c := n.FirstChild; c != nil; c = c.NextSibling {
		visitAnchorTags(c, callback)
	}
}

func getAttributeValue(n *html.Node, attribute string) (value string, found bool) {
	for _, attr := range n.Attr {
		if attr.Key == attribute {
			return attr.Val, true
		}
	}

	return "", false
}

func isElement(n *html.Node, tag string) bool {
	return n.Type == html.ElementNode && n.Data == tag
}
