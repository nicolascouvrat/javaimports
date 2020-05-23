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
	var explore func(*html.Node, bool)
	explore = func(n *html.Node, inSummary bool) {
		if inSummary {
			if n.Type == html.ElementNode && n.Data == "tbody" {
				for c := n.FirstChild; c != nil; c = c.NextSibling {
					if c.Type == html.ElementNode && c.Data == "tr" {
						addIdentifier(ci, c)
					}
				}
			}
			return
		}

		isSummary := false
		if n.Type == html.ElementNode && n.Data == "table" {
			for _, a := range n.Attr {
				if a.Key == "class" && a.Val == "memberSummary" {
					isSummary = true
				}
			}
		}

		for c := n.FirstChild; c != nil; c = c.NextSibling {
			explore(c, isSummary)
		}
	}

	explore(doc, false)
	return nil
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

	return findAllClassPrefixes(doc), nil
}

func findAllClassPrefixes(doc *html.Node) []classInfo {
	var classes = make([]classInfo, 0)
	var explore func(*html.Node)
	// The only "a" elements are the ones contained in the big list of all
	// available classes
	explore = func(n *html.Node) {
		if n.Type == html.ElementNode && n.Data == "a" {
			for _, a := range n.Attr {
				if a.Key == "href" {
					classes = append(classes, newClassInfo(a.Val))
				}
			}
		}

		for c := n.FirstChild; c != nil; c = c.NextSibling {
			explore(c)
		}
	}

	explore(doc)
	return classes
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
