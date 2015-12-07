" Vim syntax file
" Language: Construct
" Maintainer: Alex Ozdemir <aozdemir@hmc.edu>
" Latest Revision: 6 December 2015

if exists("b:current_syntax")
    finish
endif

" Keywords
syn keyword constructInclude include nextgroup=constructPath skipwhite
syn keyword constructKeywords let given return
syn keyword constructHeader construction shape
syn keyword constructBuiltinTypes point
syn keyword constructBuiltinTypes line ray circle segment
syn keyword constructBuiltinFns intersection new

let b:current_syntax = "con"

hi def link constructInclude PreProc
hi def link constructKeywords Function
hi def link constructHeader Special
hi def link constructBuiltinFns Statement
hi def link constructBuiltinTypes Type

