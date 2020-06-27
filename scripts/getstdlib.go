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
