package main

import (
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"

	"golang.org/x/net/html"
)

const (
	javaVer            = 8
	javaSEUrlBase      = "https://docs.oracle.com/javase/%d/docs/api/%s"
	allClassesPrefix   = "allclasses-noframe.html"
	outputFileTemplate = "java-%d.txt"
	// Change this depending on ulimit
	maxParallelConnections = 1000
)

var exists = struct{}{}

// classInfo contains a description of a class (or enum, or interface)
type classInfo struct {
	pkg                string
	name               string
	staticIdentifiers  map[string]interface{}
	visibleIdentifiers map[string]interface{}
}

func (ci *classInfo) output(w io.Writer) {
	header := fmt.Sprintf("pkg %s class %s", ci.pkg, ci.name)
	io.WriteString(w, header+"\n")
	for ident, _ := range ci.staticIdentifiers {
		io.WriteString(w, fmt.Sprintf("%s, static %s\n", header, ident))
	}

	for ident, _ := range ci.visibleIdentifiers {
		io.WriteString(w, fmt.Sprintf("%s, %s\n", header, ident))
	}
}

// generate an empty classInfo using a url fragment (like java/util/List.html)
func newClassInfo(prefix string) classInfo {
	pkg, name := parsePrefix(prefix)
	return classInfo{
		pkg:                pkg,
		name:               name,
		staticIdentifiers:  make(map[string]interface{}),
		visibleIdentifiers: make(map[string]interface{}),
	}
}

// splits a class URL fragment (like java/util/List.html) into a class name and
// a package path
func parsePrefix(prefix string) (pkg, className string) {
	pkgPath, prefixedClassName := filepath.Split(prefix)
	pkg = strings.ReplaceAll(filepath.Clean(pkgPath), "/", ".")
	className = strings.ReplaceAll(prefixedClassName, ".html", "")
	// The oracle documentation creates a separate page for static subclasses, with
	// class name Parent.StaticChild
	// We want to be able to address StaticChild by its own name
	if fragments := strings.Split(className, "."); len(fragments) > 1 {
		className = fragments[len(fragments)-1]
		pkg = pkg + "." + strings.Join(fragments[0:len(fragments)-1], ".")
	}

	return pkg, className
}

func main() {
	prefixes, err := getAllPrefixes()
	if err != nil {
		panic(err)
	}

	file, err := os.Create(fmt.Sprintf(outputFileTemplate, javaVer))
	if err != nil {
		panic(err)
	}

	defer file.Close()
	for class := range getAllClasses(prefixes) {
		class.output(file)
	}
}

func getAllClasses(prefixes []string) chan classInfo {
	var wg sync.WaitGroup
	classes := make(chan classInfo)
	for _, prefix := range prefixes {
		wg.Add(1)
		go func(p string) {
			defer wg.Done()
			classes <- populateClassInfo(p)
		}(prefix)
	}

	go func() {
		wg.Wait()
		close(classes)
	}()

	return classes
}

// queries the java SE documentation for a given class using its URL fragment
// (like java/util/List/html)
func populateClassInfo(prefix string) classInfo {
	raw, err := getRaw(generateJavaSEUrl(prefix))
	if err != nil {
		panic(err)
	}

	doc, err := html.Parse(strings.NewReader(string(raw)))
	if err != nil {
		panic(err)
	}

	return populateClassInfoFromHtml(prefix, doc)
}

// getAllPrefixes queries the java SE documentation and returns all classes' URL
// fragments
func getAllPrefixes() ([]string, error) {
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

// generateJavaSEUrl generates the URL for a given page of the java SE javadoc
// given a prefix, leveraging the fact that all pages use the same base URL
func generateJavaSEUrl(prefix string) string {
	return fmt.Sprintf(javaSEUrlBase, javaVer, prefix)
}

// ulimit restricts the number of maximum allowed open connections
var tr = &http.Transport{MaxConnsPerHost: maxParallelConnections}

var client = &http.Client{Transport: tr}

// getRaw returns html as a string
func getRaw(url string) ([]byte, error) {
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return nil, err
	}

	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()
	return ioutil.ReadAll(resp.Body)
}
