package main

import (
	"bytes"
	"container/list"
	"fmt"
	"net/http"

	"golang.org/x/net/html"
)

const (
	Start        = iota
	FoundHeading = iota
	FoundTable   = iota
)

func main() {
	url := "https://www.washington.edu/students/reg/1819cal.html"
	resp, _ := http.Get(url)

	state := Start

	z := html.NewTokenizer(resp.Body)

	var currentCell bytes.Buffer
	currentRow := list.New()
	rows := list.New()

ParseLoop:
	for {
		tagType := z.Next()
		if tagType == html.ErrorToken {
			fmt.Println("Error token")
			break
		}

		switch state {
		case Start:
			if tagType == html.StartTagToken {
				tagName, _ := z.TagName()
				key, val, _ := z.TagAttr()
				if string(tagName) == "h2" && string(key) == "id" && string(val) == "QS" {
					fmt.Println(string(z.Raw()))
					state = FoundHeading
				}
			}
			break
		case FoundHeading:
			if tagType == html.StartTagToken {
				tagName, _ := z.TagName()
				if string(tagName) == "table" {
					fmt.Println("Found table")
					state = FoundTable
				}
			}
			break
		case FoundTable:
			tagName, _ := z.TagName()
			if tagType == html.EndTagToken {
				switch string(tagName) {
				case "tr":
					// Save this row
					rows.PushBack(currentRow)
					break
				case "th":
				case "td":
					// Add current cell to the row and reset current cell for the next td
					currentRow.PushBack(currentCell.String())
					currentCell.Reset()
					break
				case "table":
					break ParseLoop
				}
			} else if tagType == html.TextToken {
				currentCell.Write(z.Text())
			}
		}
	}

	resp.Body.Close()
}
