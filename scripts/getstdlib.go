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
// It contains two types of information:
//	- what (public) static identifiers are there?
//	- what identifiers (methods and variables) are visible to child classes?
type classInfo struct {
	// the url fragment to combine with the base URL to access this class' info
	// page
	prefix string

	name                    string
	publicStaticIdentifiers []string
	visibleIdentifiers      []string
}

// generateJavaSEUrl from prefix
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

func getAllClasses() ([]classInfo, error) {
	raw, err := getRaw(generateJavaSEUrl(allClassesPrefix))
	if err != nil {
		return nil, err
	}

	doc, err := html.Parse(strings.NewReader(string(raw)))
	var classes = make([]classInfo, 0)
	var explore func(*html.Node)
	// The only "a" elements are the ones contained in the big list of all
	// available classes
	explore = func(n *html.Node) {
		if n.Type == html.ElementNode && n.Data == "a" {
			for _, a := range n.Attr {
				if a.Key == "href" {
					classes = append(classes, classInfo{prefix: a.Val})
				}
			}
		}

		for c := n.FirstChild; c != nil; c = c.NextSibling {
			explore(c)
		}
	}

	explore(doc)
	return classes, nil
}

func getInnerText(n *html.Node) string {
	var explore func(*html.Node) string
	explore = func(*html.Node) string {
		if n.Type == html.TextNode {
			return n.Data
		}

		if n.FirstChild != n.LastChild {
			panic("more than one child")
		}

		return explore(n.FirstChild)
	}

	return explore(n)
}

func populateClassInfo(ci classInfo) error {
	raw, err := getRaw(generateJavaSEUrl(ci.prefix))
	if err != nil {
		return err
	}

	var explore func(*html.Node, bool, bool, bool)
	explore = func(n *html.Node, inSummary, inFirstCol, inLastCol bool) {
		isSummary, isFirstCol, isLastCol := inSummary, inFirstCol, inLastCol
		if n.Type == html.ElementNode && n.Data == "table" {
			for _, a := range n.Attr {
				if a.Key == "class" && a.Val == "memberSummary" {
					isSummary = true
				}
			}
		}

		if n.Type == html.ElementNode && n.Data == "td" {
			for _, a := range n.Attr {
				if a.Key == "class" && a.Val == "colFirst" {
					isFirstCol = true
				}
			}
		}

		if n.Type == html.ElementNode && n.Data == "td" {
			for _, a := range n.Attr {
				if a.Key == "class" && a.Val == "colLast" {
					isLastCol = true
				}
			}
		}

		if n.Type == html.TextNode {
			if isFirstCol && isLastCol {
				panic("first and last col?!")
			}

			if isFirstCol {
			}
		}

		for c := n.FirstChild; c != nil; c = c.NextSibling {
			explore(c, isSummary, isFirstCol, isLastCol)
		}
	}

	return nil
}

func main() {
	allClasses, err := getAllClasses()
	if err != nil {
		panic(err)
	}

	fmt.Println(len(allClasses))
}
