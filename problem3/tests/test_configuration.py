import json
import pytest
from app.configuration import baseline, resolve
from scripts.render_bundle import compile_release


def test_environment_precedence_and_attribution(tmp_path):
    (tmp_path/'application.json').write_text(json.dumps({'LIMIT': 4}))
    values, sources = baseline(tmp_path, {'LIMIT': '9'})
    assert values == {'LIMIT': 9}
    assert sources['LIMIT']['source'] == 'environment'


def test_rules_require_every_selector_label_and_honor_priority():
    rules = [
        {'id': 'one', 'priority': 1, 'selector': {'workload': 'x'}, 'settings': {'LIMIT': 6}, 'origin': {}},
        {'id': 'two', 'priority': 2, 'selector': {'workload': 'x', 'region': 'west'}, 'settings': {'LIMIT': 8}, 'origin': {}},
    ]
    values, sources = resolve({'LIMIT': 4}, {}, {'revision':'test','rules':rules}, {'workload':'x','region':'east'})
    assert values['LIMIT'] == 6 and sources['LIMIT']['rule']=='one'
    values, _ = resolve(values, sources, {'revision':'test','rules':rules}, {'workload':'x','region':'west'})
    assert values['LIMIT'] == 8


def test_invalid_runtime_value_is_rejected():
    rule = {'id':'a','priority':1,'selector':{},'settings':{'LIMIT':-1},'origin':{}}
    with pytest.raises(ValueError):
        resolve({'LIMIT':4}, {}, {'revision':'test','rules':[rule]}, {})


def test_compiler_records_source_and_release(tmp_path):
    (tmp_path/'policy.json').write_text(json.dumps({'id':'a','priority':1,'selector':{'workload':'x'},'settings':{'LIMIT':4}}))
    manifest = tmp_path/'release.json'
    manifest.write_text(json.dumps({'revision':'test','bindings':[{'policy':'policy.json'}]}))
    bundle = compile_release(manifest, tmp_path)
    assert bundle['rules'][0]['origin']['release'] == 'test'
    assert bundle['rules'][0]['selector'] == {'workload':'x'}
