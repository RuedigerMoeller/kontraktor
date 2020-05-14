
Atomic Operations for all Updates Syntax:

```json
{
   "arrayA+": "element2Add",
   "arrayB-": "element2Remove",
   "arrayC?+": "element2AddIfNotExists"
}
```

* `_NULL_` is converted to `null` to workaround issues in null handling of some json encoding libs
* cannot update same attribute within a single record
* TODO: atomic object operations